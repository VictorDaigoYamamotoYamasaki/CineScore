package com.cinescore.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
public class RecommendationDTO {
    private Long   id;
    private String title;
    private String poster;
    private String year;
    private Double voteAverage;
    private String reason;

    // Inner classes para deserializar resposta TMDB /discover
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TmdbDiscoverResponse {
        @JsonAlias("results")
        private List<TmdbDiscoverItem> results;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TmdbDiscoverItem {
        @JsonAlias("id")
        private Long id;

        @JsonAlias("title")
        private String title;

        @JsonAlias("poster_path")
        private String posterPath;

        @JsonAlias("release_date")
        private String releaseDate;

        @JsonAlias("vote_average")
        private Double voteAverage;

        @JsonAlias("genre_ids")
        private List<Long> genreIds;
    }
}
