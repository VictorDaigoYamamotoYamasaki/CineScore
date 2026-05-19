package com.cinescore.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ReactionSummaryDTO {
    private Map<String, Long> counts;
    private List<String> myReactions;
}
