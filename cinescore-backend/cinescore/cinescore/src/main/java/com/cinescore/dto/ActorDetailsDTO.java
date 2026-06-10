package com.cinescore.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActorDetailsDTO {

    @JsonAlias("id")
    private Long id;

    @JsonAlias("name")
    private String name;

    @JsonAlias("biography")
    private String biography;

    @JsonAlias("birthday")
    private String birthday;

    @JsonAlias("place_of_birth")
    private String placeOfBirth;

    @JsonAlias("profile_path")
    private String profilePath;

    @JsonAlias("known_for_department")
    private String knownForDepartment;

    private String photo;
    private List<ActorMovieDTO> movies;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ActorMovieDTO {
        @JsonAlias("id")
        private Long id;

        @JsonAlias("title")
        private String title;

        @JsonAlias("poster_path")
        private String posterPath;

        @JsonAlias("release_date")
        private String releaseDate;

        @JsonAlias("character")
        private String character;

        @JsonAlias("vote_average")
        private Double voteAverage;

        private String poster;
        private String year;
    }
}
