package com.cinescore.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminStatsDTO {
    private long totalUsuarios;
    private long totalReviews;
}
