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
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository        reviewRepository;
    private final ReviewHistoryRepository reviewHistoryRepository;
    private final UserRepository          userRepository;
    private final ModerationService       moderationService;

    public ReviewResponseDTO criar(Long userId, ReviewRequestDTO dto) {
        User user = findUserOrThrow(userId);
        moderateTextIfPresent(dto.getReviewText());

        Review review = buildReview(user, dto);
        return ReviewResponseDTO.fromReview(reviewRepository.save(review));
    }

    public ReviewResponseDTO buscarPorId(Long reviewId) {
        return ReviewResponseDTO.fromReview(findReviewOrThrow(reviewId));
    }

    public List<ReviewResponseDTO> listarTodos() {
        return reviewRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Review::getCreatedAt).reversed())
                .map(ReviewResponseDTO::fromReview)
                .toList();
    }

    public List<ReviewResponseDTO> listarPorFilme(String movieId) {
        return reviewRepository.findByMovieId(movieId)
                .stream()
                .sorted(Comparator.comparing(Review::getCreatedAt).reversed())
                .map(ReviewResponseDTO::fromReview)
                .toList();
    }

    public List<ReviewResponseDTO> listarPorUsuario(Long userId) {
        return reviewRepository.findByUserId(userId)
                .stream()
                .sorted(Comparator.comparing(Review::getCreatedAt).reversed())
                .map(ReviewResponseDTO::fromReview)
                .toList();
    }

    public ReviewResponseDTO atualizar(Long reviewId, Long userId, ReviewUpdateDTO dto) {
        Review review = findReviewOrThrow(reviewId);
        validateOwnership(review, userId, "editar esta review");

        registrarHistoricoAlteracao(review, dto.getRating());
        atualizarCamposReview(review, dto);

        return ReviewResponseDTO.fromReview(reviewRepository.save(review));
    }

    public void deletar(Long reviewId, Long userId) {
        Review review = findReviewOrThrow(reviewId);
        validateOwnership(review, userId, "deletar esta review");
        reviewRepository.deleteById(reviewId);
    }


    private Review findReviewOrThrow(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));
    }

    private void validateOwnership(Review review, Long userId, String operation) {
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
    }
}
