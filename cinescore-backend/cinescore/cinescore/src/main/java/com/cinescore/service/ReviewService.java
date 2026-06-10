package com.cinescore.service;

import com.cinescore.dto.ReviewRequestDTO;
import com.cinescore.dto.ReviewResponseDTO;
import com.cinescore.dto.ReviewUpdateDTO;
import com.cinescore.exception.ResourceNotFoundException;
import com.cinescore.exception.UnauthorizedOperationException;
import com.cinescore.model.Review;
import com.cinescore.model.ReviewHistory;
import com.cinescore.model.User;
import com.cinescore.repository.ReviewHistoryRepository;
import com.cinescore.repository.ReviewRepository;
import com.cinescore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository        reviewRepository;
    private final ReviewHistoryRepository reviewHistoryRepository;
    private final UserRepository          userRepository;
    private final ModerationService       moderationService;

    @Transactional
    public ReviewResponseDTO criar(String userId, ReviewRequestDTO dto) {
        User user = findUserOrThrow(userId);
        moderateTextIfPresent(dto.getReviewText());

        Review review = buildReview(user, dto);
        ReviewResponseDTO saved = ReviewResponseDTO.fromReview(reviewRepository.save(review));
        log.info("Review criada: id={} userId={} movieId={}", saved.getId(), userId, dto.getMovieId());
        return saved;
    }

    @Transactional(readOnly = true)
    public ReviewResponseDTO buscarPorId(String reviewId) {
        return ReviewResponseDTO.fromReview(findReviewOrThrow(reviewId));
    }

    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> listarTodos() {
        return reviewRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ReviewResponseDTO::fromReview)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> listarPorFilme(String movieId) {
        return reviewRepository.findByMovieIdOrderByCreatedAtDesc(movieId)
                .stream()
                .map(ReviewResponseDTO::fromReview)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> listarPorUsuario(String userId) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ReviewResponseDTO::fromReview)
                .toList();
    }

    @Transactional
    public ReviewResponseDTO atualizar(String reviewId, String userId, ReviewUpdateDTO dto) {
        Review review = findReviewOrThrow(reviewId);
        validateOwnership(review, userId, "editar esta review");

        registrarHistoricoAlteracao(review, dto.getRating());
        atualizarCamposReview(review, dto);

        log.info("Review atualizada: id={} userId={}", reviewId, userId);
        return ReviewResponseDTO.fromReview(reviewRepository.save(review));
    }

    @Transactional
    public void deletar(String reviewId, String userId) {
        Review review = findReviewOrThrow(reviewId);
        validateOwnership(review, userId, "deletar esta review");
        reviewRepository.deleteById(reviewId);
        log.info("Review deletada: id={} userId={}", reviewId, userId);
    }

    @Transactional
    public ReviewResponseDTO atualizarDataAssistido(String reviewId, String userId, LocalDate watchedAt) {
        Review review = findReviewOrThrow(reviewId);
        validateOwnership(review, userId, "editar esta review");
        review.setWatchedAt(watchedAt);
        log.info("Data assistido atualizada: reviewId={} watchedAt={}", reviewId, watchedAt);
        return ReviewResponseDTO.fromReview(reviewRepository.save(review));
    }


    private Review findReviewOrThrow(String reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));
    }

    private User findUserOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));
    }

    private void validateOwnership(Review review, String userId, String operation) {
        if (!review.getUser().getId().equals(userId)) {
            throw new UnauthorizedOperationException(operation);
        }
    }

    private void moderateTextIfPresent(String text) {
        if (text != null && !text.isBlank()) {
            moderationService.verificar(text, "review");
        }
    }

    private Review buildReview(User user, ReviewRequestDTO dto) {
        return Review.builder()
                .user(user)
                .movieId(dto.getMovieId())
                .movieTitle(dto.getMovieTitle())
                .moviePoster(dto.getMoviePoster())
                .rating(dto.getRating())
                .reviewText(dto.getReviewText())
                .watchedAt(dto.getWatchedAt() != null ? dto.getWatchedAt() : LocalDate.now())
                .build();
    }

    private void registrarHistoricoAlteracao(Review review, Double novaNota) {
        ReviewHistory history = ReviewHistory.builder()
                .review(review)
                .ratingOld(review.getRating())
                .ratingNew(novaNota)
                .build();
        reviewHistoryRepository.save(history);
    }

    private void atualizarCamposReview(Review review, ReviewUpdateDTO dto) {
        review.setRating(dto.getRating());
        review.setReviewText(dto.getReviewText());
        if (dto.getWatchedAt() != null) {
            review.setWatchedAt(dto.getWatchedAt());
        }
    }
}
