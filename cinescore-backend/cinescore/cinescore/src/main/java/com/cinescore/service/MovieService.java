package com.cinescore.service;

import com.cinescore.dto.ActorDetailsDTO;
import com.cinescore.dto.RecommendationDTO;
import com.cinescore.dto.ActorSearchItemDTO;
import com.cinescore.dto.MovieDTO;
import com.cinescore.dto.MovieSearchItemDTO;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import com.cinescore.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final RestTemplate restTemplate;

    @Value("6fce4d756b5419a6a28a6e31dd51c085")
    private String apiKey;

    @Value("https://api.themoviedb.org/3")
    private String baseUrl;

    @Value("https://image.tmdb.org/t/p/w500")
    private String imageUrl;

    public List<MovieSearchItemDTO> buscarPorTitulo(String titulo) {
        String url = baseUrl + "/search/movie?query={query}&language=pt-BR&api_key={key}";

        TmdbSearchResponse response = restTemplate.getForObject(
                url, TmdbSearchResponse.class, titulo, apiKey
        );

        if (response == null || response.getResults() == null) {
            return Collections.emptyList();
        }

        return response.getResults().stream().map(item -> {
            item.setYear(item.getReleaseDate() != null && item.getReleaseDate().length() >= 4
                    ? item.getReleaseDate().substring(0, 4) : "");
            item.setPoster(item.getPosterPath() != null
                    ? imageUrl + item.getPosterPath() : null);
            return item;
        }).collect(Collectors.toList());
    }

    public MovieDTO buscarPorId(String movieId) {
        String url = baseUrl + "/movie/{id}?language=pt-BR&api_key={key}&append_to_response=credits";

        MovieDTO movie = restTemplate.getForObject(url, MovieDTO.class, movieId, apiKey);

        if (movie == null || movie.getId() == null) {
            throw new ResourceNotFoundException("Filme", "id", movieId);
        }

        movie.setPoster(movie.getPosterPath() != null ? imageUrl + movie.getPosterPath() : null);
        movie.setYear(movie.getReleaseDate() != null && movie.getReleaseDate().length() >= 4
                ? movie.getReleaseDate().substring(0, 4) : "");

        if (movie.getGenres() != null) {
            movie.setGenre(movie.getGenres().stream()
                    .map(MovieDTO.GenreDTO::getName)
                    .collect(Collectors.joining(", ")));
        }

        if (movie.getCredits() != null) {
            if (movie.getCredits().getCrew() != null) {
                movie.setDirector(movie.getCredits().getCrew().stream()
                        .filter(c -> "Director".equals(c.getJob()))
                        .map(MovieDTO.CrewDTO::getName)
                        .findFirst()
                        .orElse(null));
            }
            if (movie.getCredits().getCast() != null) {
                movie.setActors(movie.getCredits().getCast().stream()
                        .limit(5)
                        .map(MovieDTO.PersonDTO::getName)
                        .collect(Collectors.joining(", ")));
            }
        }

        return movie;
    }


    public List<RecommendationDTO> buscarTrending() {
        try {
            String url = baseUrl + "/trending/movie/week?language=pt-BR&api_key={key}";
            RecommendationDTO.TmdbDiscoverResponse resp = restTemplate.getForObject(
                    url, RecommendationDTO.TmdbDiscoverResponse.class, apiKey);

            if (resp == null || resp.getResults() == null) return Collections.emptyList();

            return resp.getResults().stream()
                    .limit(5)
                    .map(m -> {
                        RecommendationDTO dto = new RecommendationDTO();
                        dto.setId(m.getId());
                        dto.setTitle(m.getTitle());
                        dto.setPoster(m.getPosterPath() != null ? imageUrl + m.getPosterPath() : null);
                        dto.setYear(m.getReleaseDate() != null && m.getReleaseDate().length() >= 4
                                ? m.getReleaseDate().substring(0, 4) : "");
                        dto.setVoteAverage(m.getVoteAverage());
                        dto.setReason("Trending");
                        return dto;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }


    public List<ActorSearchItemDTO> buscarAtorPorNome(String nome) {
        String url = baseUrl + "/search/person?query={query}&language=pt-BR&api_key={key}";

        TmdbPersonSearchResponse response = restTemplate.getForObject(
                url, TmdbPersonSearchResponse.class, nome, apiKey
        );

        if (response == null || response.getResults() == null) {
            return Collections.emptyList();
        }

        return response.getResults().stream()
                .filter(a -> a.getProfilePath() != null)
                .map(actor -> {
                    if (actor.getProfilePath() != null) {
                        actor.setPhoto(imageUrl + actor.getProfilePath());
                    }
                    if (actor.getKnownFor() != null) {
                        String titles = actor.getKnownFor().stream()
                                .map(k -> k.getTitle() != null ? k.getTitle() : k.getName())
                                .filter(t -> t != null && !t.isBlank())
                                .limit(3)
                                .collect(Collectors.joining(", "));
                        actor.setKnownForTitles(titles);
                    }
                    return actor;
                })
                .limit(8)
                .collect(Collectors.toList());
    }

    public ActorDetailsDTO buscarDetalhesPorAtor(String actorId) {
        String detailsUrl = baseUrl + "/person/{id}?language=pt-BR&api_key={key}";
        ActorDetailsDTO actor = restTemplate.getForObject(detailsUrl, ActorDetailsDTO.class, actorId, apiKey);

        if (actor == null || actor.getId() == null) {
            throw new ResourceNotFoundException("Ator", "id", String.valueOf(actorId));
        }

        if (actor.getProfilePath() != null) {
            actor.setPhoto(imageUrl + actor.getProfilePath());
        }

        String creditsUrl = baseUrl + "/person/{id}/movie_credits?language=pt-BR&api_key={key}";

        TmdbMovieCreditsResponse credits = restTemplate.getForObject(
                creditsUrl, TmdbMovieCreditsResponse.class, actorId, apiKey
        );

        if (credits != null && credits.getCast() != null) {
            List<ActorDetailsDTO.ActorMovieDTO> movies = credits.getCast().stream()
                    .filter(m -> m.getPosterPath() != null && m.getReleaseDate() != null && !m.getReleaseDate().isBlank())
                    .map(m -> {
                        m.setPoster(imageUrl + m.getPosterPath());
                        m.setYear(m.getReleaseDate().length() >= 4 ? m.getReleaseDate().substring(0, 4) : "");
                        return m;
                    })
                    .sorted(Comparator.comparing(
                            ActorDetailsDTO.ActorMovieDTO::getReleaseDate,
                            Comparator.reverseOrder()
                    ))
                    .collect(Collectors.toList());
            actor.setMovies(movies);
        } else {
            actor.setMovies(Collections.emptyList());
        }

        return actor;
    }


    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TmdbSearchResponse {
        @JsonAlias("results")
        private List<MovieSearchItemDTO> results;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TmdbPersonSearchResponse {
        @JsonAlias("results")
        private List<ActorSearchItemDTO> results;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TmdbMovieCreditsResponse {
        @JsonAlias("cast")
        private List<ActorDetailsDTO.ActorMovieDTO> cast;
    }
}
