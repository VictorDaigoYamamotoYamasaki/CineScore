package com.cinescore.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MovieSearchItemDTO {

    @JsonAlias("id")
    private Long id;

    @JsonAlias("title")
    private String title;

    @JsonAlias("release_date")
    private String releaseDate;

    @JsonAlias("poster_path")
    private String posterPath;

    private String poster;
    private String year;
    @JsonAlias("vote_average")
    private Double voteAverage;
}
