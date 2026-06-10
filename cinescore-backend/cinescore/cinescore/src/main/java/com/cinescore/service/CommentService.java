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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private static final int MAX_COMMENT_LENGTH = 500;

    @PersistenceContext
    private EntityManager entityManager;

    private final CommentRepository  commentRepository;
    private final ModerationService  moderationService;
    private final ReactionRepository reactionRepository;
    private final ReviewRepository   reviewRepository;
    private final UserRepository     userRepository;


    @Transactional(readOnly = true)
    public List<CommentDTO> listarComentarios(String reviewId) {
        return commentRepository.findByReviewIdOrderByCreatedAtAsc(reviewId)
                .stream()
                .map(CommentDTO::from)
                .toList();
    }

    @Transactional
    public CommentDTO adicionarComentario(String reviewId, String userId, String texto) {
        validarTextoDoComentario(texto);
        moderationService.verificar(texto, "comentário");

        Review review = findReviewOrThrow(reviewId);
        User   user   = findUserOrThrow(userId);

        Comment comentario = Comment.builder()
                .review(review)
                .user(user)
                .commentText(texto.trim())
                .build();

        log.info("Comentário adicionado: reviewId={} userId={}", reviewId, userId);
        return CommentDTO.from(commentRepository.save(comentario));
    }

    @Transactional
    public void deletarComentario(String commentId, String userId) {
        Comment comentario = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comentário", commentId));

        if (!comentario.getUser().getId().equals(userId)) {
            throw new UnauthorizedOperationException("deletar este comentário");
        }
        commentRepository.deleteById(commentId);
    }


    @Transactional
    public ReactionSummaryDTO alternarReacao(String reviewId, String userId, String emoji) {
        Optional<Reaction> reacaoExistente =
                reactionRepository.findByReviewIdAndUserIdAndEmoji(reviewId, userId, emoji);

        if (reacaoExistente.isPresent()) {
            reactionRepository.deleteById(reacaoExistente.get().getId());
        } else {
            substituirReacaoAnteriorPor(reviewId, userId, emoji);
        }

        entityManager.flush();
        entityManager.clear();
        return montarResumoDeReacoes(reviewId, userId);
    }

    @Transactional(readOnly = true)
    public ReactionSummaryDTO buscarReacoes(String reviewId, String userId) {
        return montarResumoDeReacoes(reviewId, userId);
    }


    @Transactional(readOnly = true)
    public ReviewSummaryDTO buscarResumoDaReview(String reviewId, String userId) {
        ReviewSummaryDTO dto = new ReviewSummaryDTO();
        dto.setReviewId(reviewId);
        dto.setCommentCount(commentRepository.countByReviewId(reviewId));
        dto.setReactions(montarResumoDeReacoes(reviewId, userId));
        return dto;
    }


    private Review findReviewOrThrow(String reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));
    }

    private User findUserOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));
    }

    private void validarTextoDoComentario(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new InvalidInputException("comentário", "não pode ser vazio");
        }
        if (texto.trim().length() > MAX_COMMENT_LENGTH) {
            throw new InvalidInputException("comentário",
                    "deve ter no máximo " + MAX_COMMENT_LENGTH + " caracteres");
        }
    }

    private void substituirReacaoAnteriorPor(String reviewId, String userId, String novoEmoji) {
        reactionRepository.deleteByReviewIdAndUserId(reviewId, userId);

        Review review = findReviewOrThrow(reviewId);
        User   user   = findUserOrThrow(userId);

        reactionRepository.save(Reaction.builder()
                .review(review)
                .user(user)
                .emoji(novoEmoji)
                .build());
    }

    private ReactionSummaryDTO montarResumoDeReacoes(String reviewId, String userId) {
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
