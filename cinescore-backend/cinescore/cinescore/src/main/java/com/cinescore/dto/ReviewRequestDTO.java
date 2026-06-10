package com.cinescore.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReviewRequestDTO {

    @NotBlank(message = "ID do filme é obrigatório")
    private String movieId;

    private String movieTitle;
    private String moviePoster;

    @NotNull(message = "Nota é obrigatória")
    @DecimalMin(value = "0.5", message = "Nota mínima é 0,5")
    @DecimalMax(value = "5.0", message = "Nota máxima é 5,0")
    private Double rating;

    @Size(max = 2000, message = "Review deve ter no máximo 2000 caracteres")
    private String reviewText;

    private LocalDate watchedAt;
}
