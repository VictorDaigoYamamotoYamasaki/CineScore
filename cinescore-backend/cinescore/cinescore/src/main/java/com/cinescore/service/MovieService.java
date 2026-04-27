package com.cinescore.service;

import com.cinescore.dto.MovieDTO;
import com.cinescore.dto.MovieSearchItemDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final RestTemplate restTemplate;

    @Value("${omdb.api.key}")
    private String apiKey;

    @Value("${omdb.api.url}")
    private String apiUrl;

    public List<MovieSearchItemDTO> buscarPorTitulo(String titulo) {
        String url = apiUrl + "?s={s}&type=movie&apikey={apiKey}";

        OmdbSearchResponse response = restTemplate.getForObject(
                url, OmdbSearchResponse.class, titulo, apiKey
        );

        if (response == null || !"True".equals(response.getResponse())) {
            return Collections.emptyList();
        }

        return response.getSearch() != null ? response.getSearch() : Collections.emptyList();
    }

    public MovieDTO buscarPorImdbId(String imdbId) {
        String url = apiUrl + "?i={i}&plot=full&apikey={apiKey}";

        MovieDTO movie = restTemplate.getForObject(
                url, MovieDTO.class, imdbId, apiKey
        );

        if (movie == null || !"True".equals(movie.getResponse())) {
            throw new RuntimeException(
                movie != null && movie.getError() != null
                    ? movie.getError()
                    : "Filme não encontrado: " + imdbId
            );
        }

        return movie;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OmdbSearchResponse {

        @JsonProperty("Search")
        private List<MovieSearchItemDTO> search;

        @JsonProperty("Response")
        private String response;

        @JsonProperty("Error")
        private String error;
    }
}
