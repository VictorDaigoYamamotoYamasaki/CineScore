package com.cinescore.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ReactionSummaryDTO {
    // emoji → count
    private Map<String, Long> counts;
    // emojis que o usuário atual já reagiu
    private List<String> myReactions;
}
