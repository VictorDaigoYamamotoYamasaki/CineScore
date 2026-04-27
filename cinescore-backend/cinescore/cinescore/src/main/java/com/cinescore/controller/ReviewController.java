package com.cinescore.controller;

import com.cinescore.dto.ReviewRequestDTO;
import com.cinescore.dto.ReviewResponseDTO;
import com.cinescore.dto.ReviewUpdateDTO;
import com.cinescore.model.User;
import com.cinescore.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<List<ReviewResponseDTO>> listarTodos() {
        return ResponseEntity.ok(reviewService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponseDTO> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.buscarPorId(id));
    }

    @GetMapping("/movie/{imdbId}")
    public ResponseEntity<List<ReviewResponseDTO>> listarPorFilme(@PathVariable String imdbId) {
        return ResponseEntity.ok(reviewService.listarPorFilme(imdbId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReviewResponseDTO>> listarPorUsuario(@PathVariable Long userId) {
        return ResponseEntity.ok(reviewService.listarPorUsuario(userId));
    }

    @PostMapping
    public ResponseEntity<ReviewResponseDTO> criar(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ReviewRequestDTO dto) {
        return ResponseEntity.status(201).body(reviewService.criar(currentUser.getId(), dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponseDTO> atualizar(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ReviewUpdateDTO dto) {
        return ResponseEntity.ok(reviewService.atualizar(id, currentUser.getId(), dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        reviewService.deletar(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
