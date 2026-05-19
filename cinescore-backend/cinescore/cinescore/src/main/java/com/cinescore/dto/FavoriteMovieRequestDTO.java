package com.cinescore.dto;

import lombok.Data;

@Data
public class FavoriteMovieRequestDTO {
    private String movieId;
    private String title;
    private String poster;
    private String year;
}
