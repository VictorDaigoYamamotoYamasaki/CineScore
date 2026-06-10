package com.cinescore.controller;

import com.cinescore.dto.CommentDTO;
import com.cinescore.dto.CommentRequestDTO;
import com.cinescore.dto.ReactionSummaryDTO;
import com.cinescore.dto.ReviewSummaryDTO;
import com.cinescore.model.User;
import com.cinescore.service.CommentService;
import jakarta.validation.Valid;
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

    @GetMapping("/summary")
    public ResponseEntity<ReviewSummaryDTO> resumo(
            @PathVariable String reviewId,
            @AuthenticationPrincipal User currentUser) {
        String viewerId = currentUser != null ? currentUser.getId() : "anonymous";
        return ResponseEntity.ok(commentService.buscarResumoDaReview(reviewId, viewerId));
    }

    @GetMapping("/comments")
    public ResponseEntity<List<CommentDTO>> listarComentarios(@PathVariable String reviewId) {
        return ResponseEntity.ok(commentService.listarComentarios(reviewId));
    }

    @PostMapping("/comments")
    public ResponseEntity<CommentDTO> adicionarComentario(
            @PathVariable String reviewId,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CommentRequestDTO dto) {
        return ResponseEntity.ok(
                commentService.adicionarComentario(reviewId, currentUser.getId(), dto.getText()));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deletarComentario(
            @PathVariable String reviewId,
            @PathVariable String commentId,
            @AuthenticationPrincipal User currentUser) {
        commentService.deletarComentario(commentId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reactions")
    public ResponseEntity<ReactionSummaryDTO> reagir(
            @PathVariable String reviewId,
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
                commentService.alternarReacao(reviewId, currentUser.getId(), body.get("emoji")));
    }

    @GetMapping("/reactions")
    public ResponseEntity<ReactionSummaryDTO> buscarReacoes(
            @PathVariable String reviewId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(commentService.buscarReacoes(reviewId, currentUser.getId()));
    }
}
