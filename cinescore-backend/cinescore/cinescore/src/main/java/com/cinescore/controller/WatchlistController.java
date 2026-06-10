package com.cinescore.controller;

import com.cinescore.dto.WatchlistItemDTO;
import com.cinescore.dto.WatchlistRequestDTO;
import com.cinescore.model.User;
import com.cinescore.service.WatchlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    @GetMapping
    public ResponseEntity<List<WatchlistItemDTO>> listar(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(watchlistService.listarPorUsuario(currentUser.getId()));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<WatchlistItemDTO>> listarPorUsuario(
            @PathVariable String userId) {
        return ResponseEntity.ok(watchlistService.listarPorUsuario(userId));
    }

    @PostMapping
    public ResponseEntity<WatchlistItemDTO> adicionar(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody WatchlistRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(watchlistService.adicionar(currentUser.getId(), dto));
    }

    @DeleteMapping("/{movieId}")
    public ResponseEntity<Void> remover(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String movieId) {
        watchlistService.remover(currentUser.getId(), movieId);
        return ResponseEntity.noContent().build();
    }
}
