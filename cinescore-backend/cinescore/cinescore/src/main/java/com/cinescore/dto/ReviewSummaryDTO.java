package com.cinescore.dto;

import lombok.Data;

@Data
public class ReviewSummaryDTO {
    private Long reviewId;
    private long commentCount;
    private ReactionSummaryDTO reactions;
}
