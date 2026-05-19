package com.cinescore.dto;

import lombok.Data;

@Data
public class FavoriteMovieDTO {
    private Integer position;
    private String movieId;
    private String title;
    private String poster;
    private String year;
}
