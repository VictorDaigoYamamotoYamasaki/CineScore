package com.cinescore.service;

import com.cinescore.dto.MovieDTO;
import com.cinescore.dto.RecommendationDTO;
import com.cinescore.model.FavoriteMovie;
import com.cinescore.repository.FavoriteMovieRepository;
import com.cinescore.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final FavoriteMovieRepository favoriteMovieRepository;
    private final ReviewRepository        reviewRepository;
    private final RestTemplate            restTemplate;

    @Value("${tmdb.api.key}")
    private String apiKey;

    @Value("${tmdb.api.base-url}")
    private String baseUrl;

    @Value("${tmdb.api.image-url}")
    private String imageUrl;

    private static final Map<Long, String> GENRE_NAMES = Map.ofEntries(
        Map.entry(28L,    "Ação"),       Map.entry(12L,    "Aventura"),
        Map.entry(16L,    "Animação"),   Map.entry(35L,    "Comédia"),
        Map.entry(80L,    "Crime"),      Map.entry(99L,    "Documentário"),
        Map.entry(18L,    "Drama"),      Map.entry(10751L, "Família"),
        Map.entry(14L,    "Fantasia"),   Map.entry(36L,    "História"),
        Map.entry(27L,    "Terror"),     Map.entry(10402L, "Música"),
        Map.entry(9648L,  "Mistério"),   Map.entry(10749L, "Romance"),
        Map.entry(878L,   "Ficção Científica"),
        Map.entry(53L,    "Suspense"),   Map.entry(10752L, "Guerra"),
        Map.entry(37L,    "Faroeste")
    );

    private record ActorInfo(Long id, String name, int count, int minPosition) {}

    public List<RecommendationDTO> recomendar(String userId, Set<Long> excludeIds) {
        List<FavoriteMovie> favs = favoriteMovieRepository
                .findByUserIdOrderByPosition(userId).stream()
                .filter(f -> f.getMovieId() != null)
                .toList();

        if (favs.isEmpty()) return Collections.emptyList();

        DadosDosFilmesFavoritos dados = coletarDadosDosFilmesFavoritos(favs);
        if (dados.genreCount().isEmpty()) return Collections.emptyList();

        Set<Long>               blocked = buildBlockedSet(userId, excludeIds, favs);
        List<RecommendationDTO> results = new ArrayList<>();

        montarRecomendacoesPorGenero(dados.genreCount(), blocked, results);
        montarRecomendacoesPorAtores(dados.actorInfoMap(), blocked, results);
        return results;
    }

    private record DadosDosFilmesFavoritos(
            Map<Long, Integer>   genreCount,
            Map<Long, ActorInfo> actorInfoMap) {}

    private DadosDosFilmesFavoritos coletarDadosDosFilmesFavoritos(List<FavoriteMovie> favs) {
        Map<Long, Integer>   genreCount   = new LinkedHashMap<>();
        Map<Long, ActorInfo> actorInfoMap = new LinkedHashMap<>();

        for (FavoriteMovie fav : favs) {
            int pos = fav.getPosition();
            try {
                String   url   = baseUrl + "/movie/{id}?language=pt-BR&api_key={key}&append_to_response=credits";
                MovieDTO movie = restTemplate.getForObject(url, MovieDTO.class, fav.getMovieId(), apiKey);
                if (movie == null) continue;

                coletarGeneros(movie, genreCount);
                coletarAtores(movie, pos, actorInfoMap);
            } catch (Exception e) {
                log.warn("Erro ao buscar detalhes do favorito movieId={}: {}", fav.getMovieId(), e.getMessage());
            }
        }
        return new DadosDosFilmesFavoritos(genreCount, actorInfoMap);
    }

    private void coletarGeneros(MovieDTO movie, Map<Long, Integer> genreCount) {
        if (movie.getGenres() != null) {
            movie.getGenres().forEach(g -> genreCount.merge(g.getId(), 1, Integer::sum));
        }
    }

    private void coletarAtores(MovieDTO movie, int posicaoFavorito, Map<Long, ActorInfo> actorInfoMap) {
        if (movie.getCredits() == null || movie.getCredits().getCast() == null) return;
        movie.getCredits().getCast().stream().limit(3).forEach(actor -> {
            Long id = actor.getId();
            if (id == null) return;
            actorInfoMap.merge(id,
                new ActorInfo(id, actor.getName(), 1, posicaoFavorito),
                (ex, nv) -> new ActorInfo(id, ex.name(),
                    ex.count() + 1, Math.min(ex.minPosition(), posicaoFavorito)));
        });
    }

    private void montarRecomendacoesPorGenero(Map<Long, Integer> genreCount,
                                               Set<Long> blocked,
                                               List<RecommendationDTO> results) {
        List<Long> topGenres = genreCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(3).map(Map.Entry::getKey).toList();

        String genreParam = topGenres.stream().map(String::valueOf).collect(Collectors.joining(","));
        String genreLabel = topGenres.stream()
                .map(id -> GENRE_NAMES.getOrDefault(id, "")).filter(s -> !s.isEmpty())
                .collect(Collectors.joining(", "));

        fetchDiscoverByGenre(genreParam).stream()
                .filter(m -> !blocked.contains(m.getId()))
                .limit(5)
                .forEach(m -> { blocked.add(m.getId()); results.add(toDTO(m, "Baseado em " + genreLabel)); });
    }

    private void montarRecomendacoesPorAtores(Map<Long, ActorInfo> actorInfoMap,
                                               Set<Long> blocked,
                                               List<RecommendationDTO> results) {
        actorInfoMap.values().stream()
                .sorted(Comparator.comparingInt(ActorInfo::count).reversed()
                        .thenComparingInt(ActorInfo::minPosition))
                .limit(5)
                .forEach(actor ->
                    fetchDiscoverByActor(actor.id()).stream()
                            .filter(m -> !blocked.contains(m.getId()))
                            .limit(5)
                            .forEach(m -> { blocked.add(m.getId()); results.add(toDTO(m, "Com " + actor.name())); })
                );
    }


    private Set<Long> buildBlockedSet(String userId, Set<Long> excludeIds, List<FavoriteMovie> favs) {
        Set<Long> blocked = new HashSet<>(excludeIds);
        favs.stream().map(f -> safeParseLong(f.getMovieId()))
                .filter(Objects::nonNull).forEach(blocked::add);
        reviewRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(r -> safeParseLong(r.getMovieId()))
                .filter(Objects::nonNull).forEach(blocked::add);
        return blocked;
    }

    private List<RecommendationDTO.TmdbDiscoverItem> fetchDiscoverByGenre(String genreParam) {
        List<RecommendationDTO.TmdbDiscoverItem> pool = new ArrayList<>();
        for (int p = 1; p <= 3 && pool.size() < 20; p++) {
            try {
                String url = baseUrl
                        + "/discover/movie?language=pt-BR&sort_by=vote_average.desc"
                        + "&vote_count.gte=300&with_genres={genres}&page={page}&api_key={key}";
                RecommendationDTO.TmdbDiscoverResponse resp = restTemplate.getForObject(
                        url, RecommendationDTO.TmdbDiscoverResponse.class, genreParam, p, apiKey);
                if (resp != null && resp.getResults() != null) pool.addAll(resp.getResults());
            } catch (Exception e) {
                log.warn("Erro ao buscar filmes por gênero '{}' p={}: {}", genreParam, p, e.getMessage());
            }
        }
        return pool;
    }

    private List<RecommendationDTO.TmdbDiscoverItem> fetchDiscoverByActor(Long actorId) {
        List<RecommendationDTO.TmdbDiscoverItem> pool = new ArrayList<>();
        for (int p = 1; p <= 2 && pool.size() < 10; p++) {
            try {
                String url = baseUrl
                        + "/discover/movie?language=pt-BR&sort_by=vote_average.desc"
                        + "&vote_count.gte=100&with_cast={actorId}&page={page}&api_key={key}";
                RecommendationDTO.TmdbDiscoverResponse resp = restTemplate.getForObject(
                        url, RecommendationDTO.TmdbDiscoverResponse.class, actorId, p, apiKey);
                if (resp != null && resp.getResults() != null) pool.addAll(resp.getResults());
            } catch (Exception e) {
                log.warn("Erro ao buscar filmes por ator id={} p={}: {}", actorId, p, e.getMessage());
            }
        }
        return pool;
    }

    private Long safeParseLong(String value) {
        try { return Long.parseLong(value); } catch (Exception e) { return null; }
    }

    private RecommendationDTO toDTO(RecommendationDTO.TmdbDiscoverItem item, String reason) {
        RecommendationDTO dto = new RecommendationDTO();
        dto.setId(item.getId());
        dto.setTitle(item.getTitle());
        dto.setPoster(item.getPosterPath() != null ? imageUrl + item.getPosterPath() : null);
        dto.setYear(item.getReleaseDate() != null && item.getReleaseDate().length() >= 4
                ? item.getReleaseDate().substring(0, 4) : "");
        dto.setVoteAverage(item.getVoteAverage());
        dto.setReason(reason);
        return dto;
    }
}
