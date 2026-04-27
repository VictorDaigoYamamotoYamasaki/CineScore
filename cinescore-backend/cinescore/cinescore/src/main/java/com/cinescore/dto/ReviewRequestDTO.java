package com.cinescore.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReviewRequestDTO {

    @NotBlank(message = "ID do filme é obrigatório")
    private String movieImdbId;

    @NotNull(message = "Nota é obrigatória")
    @DecimalMin(value = "0.5", message = "Nota mínima é 0.5")
    @DecimalMax(value = "5.0", message = "Nota máxima é 5")
    private Double rating;

    private String reviewText;
}
