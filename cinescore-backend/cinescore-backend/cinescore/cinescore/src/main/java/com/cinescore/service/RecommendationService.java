package com.cinescore.service;

import com.cinescore.dto.MovieDTO;
import com.cinescore.dto.RecommendationDTO;
import com.cinescore.model.FavoriteMovie;
import com.cinescore.repository.FavoriteMovieRepository;
import com.cinescore.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final FavoriteMovieRepository favoriteMovieRepository;
    private final ReviewRepository        reviewRepository;
    private final RestTemplate            restTemplate;

    private static final String API_KEY   = "6fce4d756b5419a6a28a6e31dd51c085";
    private static final String BASE_URL  = "https://api.themoviedb.org/3";
    private static final String IMAGE_URL = "https://image.tmdb.org/t/p/w500";

    private static final Map<Long, String> GENRE_NAMES = Map.ofEntries(
        Map.entry(28L, "Ação"),       Map.entry(12L, "Aventura"),  Map.entry(16L, "Animação"),
        Map.entry(35L, "Comédia"),    Map.entry(80L, "Crime"),     Map.entry(99L, "Documentário"),
        Map.entry(18L, "Drama"),      Map.entry(10751L, "Família"),Map.entry(14L, "Fantasia"),
        Map.entry(36L, "História"),   Map.entry(27L, "Terror"),    Map.entry(10402L, "Música"),
        Map.entry(9648L, "Mistério"), Map.entry(10749L, "Romance"),Map.entry(878L, "Ficção Científica"),
        Map.entry(53L, "Suspense"),   Map.entry(10752L, "Guerra"), Map.entry(37L, "Faroeste")
    );

    private record ActorInfo(Long id, String name, int count, int minPosition) {}

    /**
     * @param userId     usuário logado
     * @param excludeIds IDs de filmes já exibidos anteriormente (para não repetir)
     */
    public List<RecommendationDTO> recomendar(Long userId, Set<Long> excludeIds) {

        // 1. Favoritos do usuário
        List<FavoriteMovie> favs = favoriteMovieRepository
                .findByUserIdOrderByPosition(userId).stream()
                .filter(f -> f.getMovieId() != null)
                .toList();

        if (favs.isEmpty()) return Collections.emptyList();

        // 2. Detalhes TMDB de cada favorito
        Map<Long, Integer>   genreCount   = new LinkedHashMap<>();
        Map<Long, ActorInfo> actorInfoMap = new LinkedHashMap<>();

        for (FavoriteMovie fav : favs) {
            int pos = fav.getPosition();
            try {
                String url = BASE_URL + "/movie/{id}?language=pt-BR&api_key={key}&append_to_response=credits";
                MovieDTO movie = restTemplate.getForObject(url, MovieDTO.class, fav.getMovieId(), API_KEY);
                if (movie == null) continue;

                if (movie.getGenres() != null)
                    movie.getGenres().forEach(g -> genreCount.merge(g.getId(), 1, Integer::sum));

                if (movie.getCredits() != null && movie.getCredits().getCast() != null)
                    movie.getCredits().getCast().stream().limit(3).forEach(actor -> {
                        Long id = actor.getId();
                        if (id == null) return;
                        actorInfoMap.merge(id,
                            new ActorInfo(id, actor.getName(), 1, pos),
                            (ex, nv) -> new ActorInfo(id, ex.name(),
                                ex.count() + 1, Math.min(ex.minPosition(), pos)));
                    });
            } catch (Exception ignored) {}
        }

        if (genreCount.isEmpty()) return Collections.emptyList();

        // IDs a excluir = favoritos + filmes já avaliados pelo usuário + já exibidos na tela
        Set<Long> blocked = new HashSet<>(excludeIds);
        favs.stream().map(f -> Long.parseLong(f.getMovieId())).forEach(blocked::add);
        reviewRepository.findByUserId(userId).stream()
                .map(r -> { try { return Long.parseLong(r.getMovieId()); } catch (Exception e) { return null; } })
                .filter(Objects::nonNull)
                .forEach(blocked::add);

        List<RecommendationDTO> results = new ArrayList<>();

        // ── Seção 1: Gêneros — busca páginas 1 e 2, pega os próximos 5 não exibidos ──
        List<Long> topGenres = genreCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(3).map(Map.Entry::getKey).toList();

        String genreParam = topGenres.stream().map(String::valueOf).collect(Collectors.joining(","));
        String genreLabel = topGenres.stream()
                .map(id -> GENRE_NAMES.getOrDefault(id, "")).filter(s -> !s.isEmpty())
                .collect(Collectors.joining(", "));

        List<RecommendationDTO.TmdbDiscoverItem> genrePool = new ArrayList<>();
        for (int p = 1; p <= 3 && genrePool.size() < 20; p++) {
            try {
                String url = BASE_URL
                        + "/discover/movie?language=pt-BR&sort_by=vote_average.desc"
                        + "&vote_count.gte=300&with_genres={genres}&page={page}&api_key={key}";
                RecommendationDTO.TmdbDiscoverResponse resp = restTemplate.getForObject(
                        url, RecommendationDTO.TmdbDiscoverResponse.class, genreParam, p, API_KEY);
                if (resp != null && resp.getResults() != null)
                    genrePool.addAll(resp.getResults());
            } catch (Exception ignored) {}
        }

        genrePool.stream()
                .filter(m -> !blocked.contains(m.getId()))
                .limit(5)
                .forEach(m -> {
                    blocked.add(m.getId());
                    results.add(toDTO(m, "Baseado em " + genreLabel));
                });

        // ── Seção 2: Atores — 5 filmes por ator (top 5 atores mais frequentes) ──
        List<ActorInfo> topActors = actorInfoMap.values().stream()
                .sorted(Comparator.comparingInt(ActorInfo::count).reversed()
                        .thenComparingInt(ActorInfo::minPosition))
                .limit(5).toList();

        for (ActorInfo actor : topActors) {
            List<RecommendationDTO.TmdbDiscoverItem> actorPool = new ArrayList<>();
            for (int p = 1; p <= 2 && actorPool.size() < 10; p++) {
                try {
                    String url = BASE_URL
                            + "/discover/movie?language=pt-BR&sort_by=vote_average.desc"
                            + "&vote_count.gte=100&with_cast={actorId}&page={page}&api_key={key}";
                    RecommendationDTO.TmdbDiscoverResponse resp = restTemplate.getForObject(
                            url, RecommendationDTO.TmdbDiscoverResponse.class, actor.id(), p, API_KEY);
                    if (resp != null && resp.getResults() != null)
                        actorPool.addAll(resp.getResults());
                } catch (Exception ignored) {}
            }

            actorPool.stream()
                    .filter(m -> !blocked.contains(m.getId()))
                    .limit(5)
                    .forEach(m -> {
                        blocked.add(m.getId());
                        results.add(toDTO(m, "Com " + actor.name()));
                    });
        }

        return results;
    }

    private RecommendationDTO toDTO(RecommendationDTO.TmdbDiscoverItem item, String reason) {
        RecommendationDTO dto = new RecommendationDTO();
        dto.setId(item.getId());
        dto.setTitle(item.getTitle());
        dto.setPoster(item.getPosterPath() != null ? IMAGE_URL + item.getPosterPath() : null);
        dto.setYear(item.getReleaseDate() != null && item.getReleaseDate().length() >= 4
                ? item.getReleaseDate().substring(0, 4) : "");
        dto.setVoteAverage(item.getVoteAverage());
        dto.setReason(reason);
        return dto;
    }
}
