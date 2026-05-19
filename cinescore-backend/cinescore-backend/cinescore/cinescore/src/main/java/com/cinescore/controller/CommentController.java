package com.cinescore.controller;

import com.cinescore.dto.CommentDTO;
import com.cinescore.dto.ReactionSummaryDTO;
import com.cinescore.dto.ReviewSummaryDTO;
import com.cinescore.model.User;
import com.cinescore.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews/{reviewId}")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // ── Resumo (comentários + reações) ───────────────────────────────────────
    @GetMapping("/summary")
    public ResponseEntity<ReviewSummaryDTO> resumo(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(commentService.buscarResumoDaReview(reviewId, currentUser.getId()));
    }

    // ── Comentários ──────────────────────────────────────────────────────────
    @GetMapping("/comments")
    public ResponseEntity<List<CommentDTO>> listar(@PathVariable Long reviewId) {
        return ResponseEntity.ok(commentService.listarComentarios(reviewId));
    }

    @PostMapping("/comments")
    public ResponseEntity<CommentDTO> adicionar(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
                commentService.adicionarComentario(reviewId, currentUser.getId(), body.get("text")));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deletar(
            @PathVariable Long reviewId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal User currentUser) {
        commentService.deletarComentario(commentId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    // ── Reações ───────────────────────────────────────────────────────────────
    @PostMapping("/reactions")
    public ResponseEntity<ReactionSummaryDTO> reagir(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
                commentService.alternarReacao(reviewId, currentUser.getId(), body.get("emoji")));
    }

    @GetMapping("/reactions")
    public ResponseEntity<ReactionSummaryDTO> reacoes(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(commentService.buscarReacoes(reviewId, currentUser.getId()));
    }
}
