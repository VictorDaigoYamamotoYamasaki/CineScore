package com.cinescore.service;

import com.cinescore.dto.FavoriteMovieDTO;
import com.cinescore.dto.FavoriteMovieRequestDTO;
import com.cinescore.dto.ProfileDTO;
import com.cinescore.dto.ReviewResponseDTO;
import com.cinescore.exception.InvalidInputException;
import com.cinescore.exception.ResourceNotFoundException;
import com.cinescore.model.FavoriteMovie;
import com.cinescore.model.Review;
import com.cinescore.model.User;
import com.cinescore.repository.FavoriteMovieRepository;
import com.cinescore.repository.FollowerRepository;
import com.cinescore.repository.ReviewRepository;
import com.cinescore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final int MAX_FAVORITE_SLOTS = 5;
    private static final int MIN_POSITION       = 1;

    private final UserRepository          userRepository;
    private final ReviewRepository        reviewRepository;
    private final FavoriteMovieRepository favoriteMovieRepository;
    private final FollowerRepository      followerRepository;

    public ProfileDTO buscarPerfil(Long userId, Long currentUserId) {
        User user = findUserOrThrow(userId);

        ProfileDTO profile = new ProfileDTO();
        profile.setUserId(user.getId());
        profile.setName(user.getName());
        profile.setEmail(user.getEmail());
        profile.setFollowerCount(followerRepository.countByFollowedId(userId));
        profile.setFollowingCount(followerRepository.countByFollowerId(userId));
        profile.setFollowing(isViewerFollowingTarget(currentUserId, userId));
        profile.setFavorites(buildFavoriteSlots(userId));
        profile.setReviews(buscarReviewsOrdenadas(userId));

        return profile;
    }

    @Transactional
    public FavoriteMovieDTO salvarFavorito(Long userId, Integer position, FavoriteMovieRequestDTO req) {
        validatePosition(position);
        User user = findUserOrThrow(userId);

        removerSlotAtual(userId, position);
        removerFilmeDuplicado(userId, req.getMovieId());

        favoriteMovieRepository.save(FavoriteMovie.builder()
                .user(user)
                .movieId(req.getMovieId())
                .position(position)
                .build());

        return buildFavoriteMovieDTO(position, req);
    }

    @Transactional
    public void removerFavorito(Long userId, Integer position) {
        favoriteMovieRepository.findByUserIdAndPosition(userId, position)
                .ifPresent(favoriteMovieRepository::delete);
    }


    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));
    }

    private void validatePosition(Integer position) {
        if (position < MIN_POSITION || position > MAX_FAVORITE_SLOTS) {
            throw new InvalidInputException("posição",
                    "deve ser entre " + MIN_POSITION + " e " + MAX_FAVORITE_SLOTS);
        }
    }

    private boolean isViewerFollowingTarget(Long viewerId, Long targetId) {
        return viewerId != null
                && !viewerId.equals(targetId)
                && followerRepository.existsByFollowerIdAndFollowedId(viewerId, targetId);
    }

    private List<FavoriteMovieDTO> buildFavoriteSlots(Long userId) {
        List<FavoriteMovie> savedFavorites = favoriteMovieRepository
                .findByUserIdOrderByPosition(userId);

        List<FavoriteMovieDTO> slots = new ArrayList<>();
        for (int position = MIN_POSITION; position <= MAX_FAVORITE_SLOTS; position++) {
            slots.add(buildSlot(savedFavorites, position));
        }
        return slots;
    }

    private FavoriteMovieDTO buildSlot(List<FavoriteMovie> savedFavorites, int position) {
        FavoriteMovieDTO dto = new FavoriteMovieDTO();
        dto.setPosition(position);

        savedFavorites.stream()
                .filter(f -> f.getPosition() == position)
                .findFirst()
                .ifPresent(f -> dto.setMovieId(f.getMovieId()));

        return dto;
    }

    private List<ReviewResponseDTO> buscarReviewsOrdenadas(Long userId) {
        return reviewRepository.findByUserId(userId)
                .stream()
                .sorted(Comparator.comparing(Review::getCreatedAt).reversed())
                .map(ReviewResponseDTO::fromReview)
                .toList();
    }

    private void removerSlotAtual(Long userId, Integer position) {
        favoriteMovieRepository.findByUserIdAndPosition(userId, position)
                .ifPresent(favoriteMovieRepository::delete);
    }

    private void removerFilmeDuplicado(Long userId, String movieId) {
        favoriteMovieRepository.findByUserIdOrderByPosition(userId)
                .stream()
                .filter(f -> f.getMovieId().equals(movieId))
                .forEach(favoriteMovieRepository::delete);
    }

    private FavoriteMovieDTO buildFavoriteMovieDTO(Integer position, FavoriteMovieRequestDTO req) {
        FavoriteMovieDTO dto = new FavoriteMovieDTO();
        dto.setPosition(position);
        dto.setMovieId(req.getMovieId());
        dto.setTitle(req.getTitle());
        dto.setPoster(req.getPoster());
        dto.setYear(req.getYear());
        return dto;
    }
}
