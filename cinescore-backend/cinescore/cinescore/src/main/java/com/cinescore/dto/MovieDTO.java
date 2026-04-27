package com.cinescore.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MovieDTO {

    @JsonAlias("imdbID")
    private String imdbId;

    @JsonAlias("Title")
    private String title;

    @JsonAlias("Year")
    private String year;

    @JsonAlias("Genre")
    private String genre;

    @JsonAlias("Director")
    private String director;

    @JsonAlias("Actors")
    private String actors;

    @JsonAlias("Plot")
    private String plot;

    @JsonAlias("Poster")
    private String poster;

    @JsonAlias("imdbRating")
    private String imdbRating;

    @JsonAlias("Runtime")
    private String runtime;

    @JsonAlias("Rated")
    private String rated;

    @JsonAlias("Response")
    private String response;

    @JsonAlias("Error")
    private String error;
}
