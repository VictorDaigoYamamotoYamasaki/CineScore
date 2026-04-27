package com.cinescore.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MovieSearchItemDTO {

    @JsonAlias("imdbID")
    private String imdbId;

    @JsonAlias("Title")
    private String title;

    @JsonAlias("Year")
    private String year;

    @JsonAlias("Type")
    private String type;

    @JsonAlias("Poster")
    private String poster;
}
