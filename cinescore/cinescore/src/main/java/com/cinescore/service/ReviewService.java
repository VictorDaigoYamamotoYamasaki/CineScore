package com.cinescore.service;

import com.cinescore.dto.ReviewRequestDTO;
import com.cinescore.dto.ReviewResponseDTO;
import com.cinescore.model.Review;
import com.cinescore.model.User;
import com.cinescore.repository.ReviewRepository;
import com.cinescore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public ReviewResponseDTO criar(Long userId, ReviewRequestDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com id: " + userId));

        if (reviewRepository.existsByUserIdAndMovieImdbId(userId, dto.getMovieImdbId())) {
            throw new RuntimeException("Você já avaliou este filme");
        }

        Review review = Review.builder()
                .user(user)
                .movieImdbId(dto.getMovieImdbId())
                .rating(dto.getRating())
                .reviewText(dto.getReviewText())
                .build();

        return ReviewResponseDTO.fromReview(reviewRepository.save(review));
    }

    public ReviewResponseDTO buscarPorId(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review não encontrada com id: " + id));
        return ReviewResponseDTO.fromReview(review);
    }

    public List<ReviewResponseDTO> listarPorFilme(String movieImdbId) {
        return reviewRepository.findByMovieImdbId(movieImdbId)
                .stream()
                .map(ReviewResponseDTO::fromReview)
                .toList();
    }

    public List<ReviewResponseDTO> listarPorUsuario(Long userId) {
        return reviewRepository.findByUserId(userId)
                .stream()
                .map(ReviewResponseDTO::fromReview)
                .toList();
    }

    public ReviewResponseDTO atualizar(Long id, Long userId, ReviewRequestDTO dto) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review não encontrada com id: " + id));

        if (!review.getUser().getId().equals(userId)) {
            throw new RuntimeException("Sem permissão para editar esta review");
        }

        review.setRating(dto.getRating());
        review.setReviewText(dto.getReviewText());

        return ReviewResponseDTO.fromReview(reviewRepository.save(review));
    }

    public void deletar(Long id, Long userId) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review não encontrada com id: " + id));

        if (!review.getUser().getId().equals(userId)) {
            throw new RuntimeException("Sem permissão para deletar esta review");
        }

        reviewRepository.deleteById(id);
    }
}
