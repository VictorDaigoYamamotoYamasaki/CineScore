package com.cinescore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReviewsPerDayDTO {
    private String date;   // ex: "19/05"
    private Long   count;
}
