package com.cinescore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WatchlistRequestDTO {

    @NotBlank(message = "ID do filme é obrigatório")
    @Size(max = 20, message = "ID do filme deve ter no máximo 20 caracteres")
    private String movieId;
    private String movieTitle;
    private String moviePoster;
    private String movieYear;
    private Double movieVoteAverage;
}
