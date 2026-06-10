package com.cinescore.dto;

import lombok.Data;

@Data
public class RatingExtremesDTO {
    private PopularMovieDTO highest;
    private PopularMovieDTO lowest;
}
