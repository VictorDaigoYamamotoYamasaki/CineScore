package com.cinescore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RatingDistributionDTO {
    private Double rating;
    private Long   count;
}
