package com.cinescore.controller;

import com.cinescore.dto.RecommendationDTO;
import com.cinescore.model.User;
import com.cinescore.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/me")
    public ResponseEntity<List<RecommendationDTO>> minhasRecomendacoes(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "") String exclude) {

        Set<Long> excludeIds = exclude.isBlank()
                ? Collections.emptySet()
                : Arrays.stream(exclude.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toSet());

        return ResponseEntity.ok(recommendationService.recomendar(currentUser.getId(), excludeIds));
    }
}
