package com.cinescore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReviewsPerDayDTO {
    private String date;
    private Long   count;
}
