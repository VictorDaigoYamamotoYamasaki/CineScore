package com.cinescore.service;

import com.cinescore.dto.CommentDTO;
import com.cinescore.dto.ReactionSummaryDTO;
import com.cinescore.dto.ReviewSummaryDTO;
import com.cinescore.exception.InvalidInputException;
import com.cinescore.exception.ResourceNotFoundException;
import com.cinescore.exception.UnauthorizedOperationException;
import com.cinescore.model.Comment;
import com.cinescore.model.Reaction;
import com.cinescore.model.Review;
import com.cinescore.model.User;
import com.cinescore.repository.CommentRepository;
import com.cinescore.repository.ReactionRepository;
import com.cinescore.repository.ReviewRepository;
import com.cinescore.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    @PersistenceContext
    private EntityManager entityManager;

    private final CommentRepository  commentRepository;
    private final ModerationService  moderationService;
    private final ReactionRepository reactionRepository;
    private final ReviewRepository   reviewRepository;
    private final UserRepository     userRepository;


    public List<CommentDTO> listarComentarios(Long reviewId) {
        return commentRepository.findByReviewIdOrderByCreatedAtAsc(reviewId)
                .stream()
                .map(CommentDTO::from)
                .toList();
    }

    @Transactional
    public CommentDTO adicionarComentario(Long reviewId, Long userId, String texto) {
        validateTextoNaoVazio(texto);
        moderationService.verificar(texto, "comentário");

        Review review = findReviewOrThrow(reviewId);
        User   user   = findUserOrThrow(userId);

        Comment comment = Comment.builder()
                .review(review)
                .user(user)
                .commentText(texto.trim())
                .build();

        return CommentDTO.from(commentRepository.save(comment));
    }

    @Transactional
    public void deletarComentario(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comentário", commentId));

        if (!comment.getUser().getId().equals(userId)) {
            throw new UnauthorizedOperationException("deletar este comentário");
        }
        commentRepository.deleteById(commentId);
    }


    @Transactional
    public ReactionSummaryDTO alternarReacao(Long reviewId, Long userId, String emoji) {
        Optional<Reaction> reacaoExistente = reactionRepository
                .findByReviewIdAndUserIdAndEmoji(reviewId, userId, emoji);

        if (reacaoExistente.isPresent()) {
            removerReacao(reacaoExistente.get().getId());
        } else {
            substituirReacaoAnteriorPor(reviewId, userId, emoji);
        }

        flushAndClearPersistenceContext();
        return buildReactionSummary(reviewId, userId);
    }

    public ReactionSummaryDTO buscarReacoes(Long reviewId, Long userId) {
        return buildReactionSummary(reviewId, userId);
    }


    public ReviewSummaryDTO buscarResumoDaReview(Long reviewId, Long userId) {
        ReviewSummaryDTO dto = new ReviewSummaryDTO();
        dto.setReviewId(reviewId);
        dto.setCommentCount(commentRepository.countByReviewId(reviewId));
        dto.setReactions(buildReactionSummary(reviewId, userId));
        return dto;
    }


    private Review findReviewOrThrow(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));
    }

    private void validateTextoNaoVazio(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new InvalidInputException("texto", "não pode ser vazio");
        }
    }

    private void removerReacao(Long reacaoId) {
        reactionRepository.deleteById(reacaoId);
    }

    private void substituirReacaoAnteriorPor(Long reviewId, Long userId, String novoEmoji) {
        removerTodasReacoesDoUsuario(reviewId, userId);

        Review review = findReviewOrThrow(reviewId);
        User   user   = findUserOrThrow(userId);

        reactionRepository.save(Reaction.builder()
                .review(review)
                .user(user)
                .emoji(novoEmoji)
                .build());
    }

    private void removerTodasReacoesDoUsuario(Long reviewId, Long userId) {
        reactionRepository.findByReviewId(reviewId)
                .stream()
                .filter(r -> r.getUser().getId().equals(userId))
                .forEach(r -> reactionRepository.deleteById(r.getId()));
    }

    private void flushAndClearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }

    private ReactionSummaryDTO buildReactionSummary(Long reviewId, Long userId) {
        List<Reaction> todasReacoes = reactionRepository.findByReviewId(reviewId);

        Map<String, Long> contagemPorEmoji = todasReacoes.stream()
                .collect(Collectors.groupingBy(Reaction::getEmoji, Collectors.counting()));

        List<String> minhasReacoes = todasReacoes.stream()
                .filter(r -> r.getUser().getId().equals(userId))
                .map(Reaction::getEmoji)
                .toList();

        ReactionSummaryDTO dto = new ReactionSummaryDTO();
        dto.setCounts(contagemPorEmoji);
        dto.setMyReactions(minhasReacoes);
        return dto;
    }
}
