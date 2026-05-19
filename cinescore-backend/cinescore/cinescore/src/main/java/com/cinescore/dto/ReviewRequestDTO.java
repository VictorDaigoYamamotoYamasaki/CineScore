package com.cinescore.dto;

import lombok.Data;

@Data
public class ReviewRequestDTO {
    private String movieId;
    private String movieTitle;
    private String moviePoster;
    private Double rating;
    private String reviewText;
}
