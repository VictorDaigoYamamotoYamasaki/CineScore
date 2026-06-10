package com.cinescore.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MovieDTO {

    @JsonAlias("id")
    private Long id;

    @JsonAlias("title")
    private String title;

    @JsonAlias("release_date")
    private String releaseDate;

    @JsonAlias("overview")
    private String overview;

    @JsonAlias("poster_path")
    private String posterPath;

    @JsonAlias("vote_average")
    private Double voteAverage;

    @JsonAlias("runtime")
    private Integer runtime;

    @JsonAlias("genres")
    private List<GenreDTO> genres;

    @JsonAlias("credits")
    private CreditsDTO credits;

    private String poster;
    private String year;
    private String genre;
    private String director;
    private String actors;
    private String certification;

    @JsonAlias("release_dates")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private ReleaseDatesWrapper releaseDates;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GenreDTO {
        @JsonAlias("id")
        private Long id;

        @JsonAlias("name")
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CreditsDTO {
        @JsonAlias("cast")
        private List<PersonDTO> cast;

        @JsonAlias("crew")
        private List<CrewDTO> crew;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PersonDTO {
        @JsonAlias("id")
        private Long id;

        @JsonAlias("name")
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CrewDTO {
        @JsonAlias("name")
        private String name;

        @JsonAlias("job")
        private String job;
    }

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReleaseDatesWrapper {

        @JsonAlias("results")
        private java.util.List<CountryRelease> results;

        @lombok.Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class CountryRelease {

            @JsonAlias("iso_3166_1")
            private String isoCode;

            @JsonAlias("release_dates")
            private java.util.List<CertificationEntry> releaseDates;

            @lombok.Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class CertificationEntry {
                private String certification;
            }
        }
    }
}
