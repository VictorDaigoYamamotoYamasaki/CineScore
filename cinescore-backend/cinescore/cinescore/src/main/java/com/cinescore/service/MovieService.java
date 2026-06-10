package com.cinescore.service;

import com.cinescore.dto.ActorDetailsDTO;
import com.cinescore.dto.ActorSearchItemDTO;
import com.cinescore.dto.MovieDTO;
import com.cinescore.dto.MovieSearchItemDTO;
import com.cinescore.dto.RecommendationDTO;
import com.cinescore.exception.ResourceNotFoundException;
import com.cinescore.model.MovieCache;
import com.cinescore.repository.MovieCacheRepository;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieService {

    private final RestTemplate         restTemplate;
    private final MovieCacheRepository movieCacheRepository;

    @Value("${tmdb.api.key}")
    private String apiKey;

    @Value("${tmdb.api.base-url}")
    private String baseUrl;

    @Value("${tmdb.api.image-url}")
    private String imageUrl;


    public List<MovieSearchItemDTO> buscarPorTitulo(String titulo) {
        String url = baseUrl + "/search/movie?query={query}&language=pt-BR"
                + "&include_image_language=pt-BR,en,null&api_key={key}";

        TmdbSearchResponse response = restTemplate.getForObject(
                url, TmdbSearchResponse.class, titulo, apiKey);

        if (response == null || response.getResults() == null) return Collections.emptyList();

        return response.getResults().stream().map(item -> {
            item.setYear(extrairAno(item.getReleaseDate()));
            item.setPoster(buildPosterUrl(item.getPosterPath()));
            return item;
        }).collect(Collectors.toList());
    }

    @Cacheable(value = "tmdb-movies", key = "#movieId")
    public MovieDTO buscarPorId(String movieId) {
        try {
            MovieDTO movie = buscarDoTmdb(movieId);
            salvarNoCache(movie);
            return movie;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.warn("TMDB indisponível — usando cache local para movieId={}: {}", movieId, e.getMessage());
            return buscarDoCacheOuLancar(movieId);
        }
    }

    @Cacheable("tmdb-trending")
    public List<RecommendationDTO> buscarTrending() {
        try {
            String url = baseUrl + "/trending/movie/week?language=pt-BR"
                    + "&include_image_language=pt-BR,en,null&api_key={key}";
            RecommendationDTO.TmdbDiscoverResponse resp = restTemplate.getForObject(
                    url, RecommendationDTO.TmdbDiscoverResponse.class, apiKey);

            if (resp == null || resp.getResults() == null) return Collections.emptyList();

            return resp.getResults().stream().limit(5).map(m -> {
                RecommendationDTO dto = new RecommendationDTO();
                dto.setId(m.getId());
                dto.setTitle(m.getTitle());
                dto.setPoster(buildPosterUrl(m.getPosterPath()));
                dto.setYear(extrairAno(m.getReleaseDate()));
                dto.setVoteAverage(m.getVoteAverage());
                dto.setReason("Trending");
                return dto;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Erro ao buscar trending: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<ActorSearchItemDTO> buscarAtorPorNome(String nome) {
        String url = baseUrl + "/search/person?query={query}&language=pt-BR&api_key={key}";
        TmdbPersonSearchResponse response = restTemplate.getForObject(
                url, TmdbPersonSearchResponse.class, nome, apiKey);

        if (response == null || response.getResults() == null) return Collections.emptyList();

        return response.getResults().stream()
                .filter(a -> a.getProfilePath() != null)
                .map(actor -> {
                    actor.setPhoto(imageUrl + actor.getProfilePath());
                    if (actor.getKnownFor() != null) {
                        actor.setKnownForTitles(actor.getKnownFor().stream()
                                .map(k -> k.getTitle() != null ? k.getTitle() : k.getName())
                                .filter(t -> t != null && !t.isBlank())
                                .limit(3)
                                .collect(Collectors.joining(", ")));
                    }
                    return actor;
                })
                .limit(8)
                .collect(Collectors.toList());
    }

    public ActorDetailsDTO buscarDetalhesPorAtor(String actorId) {
        ActorDetailsDTO actor = fetchAtorOuLancar(actorId);
        if (actor.getProfilePath() != null) {
            actor.setPhoto(imageUrl + actor.getProfilePath());
        }
        actor.setMovies(fetchFilmografiaDoAtor(actorId));
        return actor;
    }


    private MovieDTO buscarDoTmdb(String movieId) {
        log.debug("Buscando filme TMDB id={}", movieId);
        String url = baseUrl + "/movie/{id}?language=pt-BR"
                + "&include_image_language=pt-BR,en,null"
                + "&api_key={key}&append_to_response=credits,release_dates";

        MovieDTO movie = restTemplate.getForObject(url, MovieDTO.class, movieId, apiKey);

        if (movie == null || movie.getId() == null) {
            throw new ResourceNotFoundException("Filme", "id", movieId);
        }
        if (movie.getPosterPath() == null) {
            movie.setPosterPath(buscarPosterSemIdioma(movieId));
        }
        enriquecerFilme(movie);
        return movie;
    }

    private void salvarNoCache(MovieDTO movie) {
        try {
            MovieCache cache = MovieCache.builder()
                    .movieId(String.valueOf(movie.getId()))
                    .title(movie.getTitle())
                    .year(movie.getYear())
                    .poster(movie.getPoster())
                    .synopsis(movie.getOverview())
                    .genres(movie.getGenre())
                    .director(movie.getDirector())
                    .actors(movie.getActors())
                    .runtime(movie.getRuntime())
                    .voteAverage(movie.getVoteAverage())
                    .certification(movie.getCertification())
                    .build();
            movieCacheRepository.save(cache);
        } catch (Exception e) {
            log.warn("Falha ao salvar filme no cache local movieId={}: {}", movie.getId(), e.getMessage());
        }
    }

    private MovieDTO buscarDoCacheOuLancar(String movieId) {
        MovieCache cached = movieCacheRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Filme", "id", movieId));

        log.info("Servindo movieId={} do cache local (TMDB indisponível)", movieId);
        return montarDtoDoCache(cached);
    }

    private MovieDTO montarDtoDoCache(MovieCache cached) {
        MovieDTO dto = new MovieDTO();
        dto.setTitle(cached.getTitle());
        dto.setYear(cached.getYear());
        dto.setPoster(cached.getPoster());
        dto.setOverview(cached.getSynopsis());
        dto.setGenre(cached.getGenres());
        dto.setDirector(cached.getDirector());
        dto.setActors(cached.getActors());
        dto.setRuntime(cached.getRuntime());
        dto.setVoteAverage(cached.getVoteAverage());
        dto.setCertification(cached.getCertification());
        return dto;
    }

    private ActorDetailsDTO fetchAtorOuLancar(String actorId) {
        String url   = baseUrl + "/person/{id}?language=pt-BR&api_key={key}";
        ActorDetailsDTO actor = restTemplate.getForObject(url, ActorDetailsDTO.class, actorId, apiKey);
        if (actor == null || actor.getId() == null) {
            throw new ResourceNotFoundException("Ator", "id", actorId);
        }
        return actor;
    }

    private List<ActorDetailsDTO.ActorMovieDTO> fetchFilmografiaDoAtor(String actorId) {
        String url = baseUrl + "/person/{id}/movie_credits?language=pt-BR&api_key={key}";
        TmdbMovieCreditsResponse credits = restTemplate.getForObject(
                url, TmdbMovieCreditsResponse.class, actorId, apiKey);

        if (credits == null || credits.getCast() == null) return Collections.emptyList();

        return credits.getCast().stream()
                .filter(m -> m.getPosterPath() != null
                        && m.getReleaseDate() != null
                        && !m.getReleaseDate().isBlank())
                .map(m -> {
                    m.setPoster(imageUrl + m.getPosterPath());
                    m.setYear(extrairAno(m.getReleaseDate()));
                    return m;
                })
                .sorted(Comparator.comparing(
                        ActorDetailsDTO.ActorMovieDTO::getReleaseDate,
                        Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }


    private void enriquecerFilme(MovieDTO movie) {
        movie.setPoster(buildPosterUrl(movie.getPosterPath()));
        movie.setYear(extrairAno(movie.getReleaseDate()));
        movie.setCertification(extrairClassificacao(movie));
        popularGeneros(movie);
        popularDiretorEAtores(movie);
    }

    private void popularGeneros(MovieDTO movie) {
        if (movie.getGenres() == null) return;
        movie.setGenre(movie.getGenres().stream()
                .map(MovieDTO.GenreDTO::getName)
                .collect(Collectors.joining(", ")));
    }

    private void popularDiretorEAtores(MovieDTO movie) {
        if (movie.getCredits() == null) return;
        if (movie.getCredits().getCrew() != null) {
            movie.setDirector(movie.getCredits().getCrew().stream()
                    .filter(c -> "Director".equals(c.getJob()))
                    .map(MovieDTO.CrewDTO::getName)
                    .findFirst().orElse(null));
        }
        if (movie.getCredits().getCast() != null) {
            movie.setActors(movie.getCredits().getCast().stream()
                    .limit(5)
                    .map(MovieDTO.PersonDTO::getName)
                    .collect(Collectors.joining(", ")));
        }
    }

    private String extrairClassificacao(MovieDTO movie) {
        if (movie.getReleaseDates() == null
                || movie.getReleaseDates().getResults() == null) return null;

        return movie.getReleaseDates().getResults().stream()
                .filter(r -> "BR".equals(r.getIsoCode()))
                .findFirst()
                .flatMap(country -> Optional.ofNullable(country.getReleaseDates())
                        .flatMap(datas -> datas.stream()
                                .filter(d -> d.getCertification() != null
                                          && !d.getCertification().isBlank())
                                .findFirst()))
                .map(MovieDTO.ReleaseDatesWrapper.CountryRelease.CertificationEntry::getCertification)
                .orElse(null);
    }

    private String buscarPosterSemIdioma(String movieId) {
        try {
            String url = baseUrl + "/movie/{id}?api_key={key}";
            MovieDTO fallback = restTemplate.getForObject(url, MovieDTO.class, movieId, apiKey);
            if (fallback != null && fallback.getPosterPath() != null) {
                log.debug("Pôster obtido via fallback sem idioma para movieId={}", movieId);
                return fallback.getPosterPath();
            }
        } catch (Exception e) {
            log.warn("Fallback de pôster falhou para movieId={}: {}", movieId, e.getMessage());
        }
        return null;
    }

    private String buildPosterUrl(String posterPath) {
        return posterPath != null ? imageUrl + posterPath : null;
    }

    private String extrairAno(String releaseDate) {
        return releaseDate != null && releaseDate.length() >= 4
                ? releaseDate.substring(0, 4) : "";
    }


    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TmdbSearchResponse {
        @JsonAlias("results") private List<MovieSearchItemDTO> results;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TmdbPersonSearchResponse {
        @JsonAlias("results") private List<ActorSearchItemDTO> results;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TmdbMovieCreditsResponse {
        @JsonAlias("cast") private List<ActorDetailsDTO.ActorMovieDTO> cast;
    }
}
