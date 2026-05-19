package com.cinescore.dto;

import lombok.Data;

@Data
public class PopularMovieDTO {
    private String movieId;
    private String movieTitle;
    private String moviePoster;
    private long   reviewCount;
    private double avgRating;
}
