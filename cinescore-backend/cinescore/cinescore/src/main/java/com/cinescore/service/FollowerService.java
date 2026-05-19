package com.cinescore.service;

import com.cinescore.dto.FollowUserDTO;
import com.cinescore.exception.ResourceNotFoundException;
import com.cinescore.exception.SelfFollowException;
import com.cinescore.model.Follower;
import com.cinescore.model.User;
import com.cinescore.repository.FollowerRepository;
import com.cinescore.repository.ReviewRepository;
import com.cinescore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowerService {

    private final FollowerRepository followerRepository;
    private final UserRepository     userRepository;
    private final ReviewRepository   reviewRepository;

    @Transactional
    public void seguir(Long followerId, Long followedId) {
        validateNaoSegueASiMesmo(followerId, followedId);

        if (jaSegue(followerId, followedId)) return;

        User follower = findUserOrThrow(followerId);
        User followed = findUserOrThrow(followedId);

        followerRepository.save(Follower.builder()
                .follower(follower)
                .followed(followed)
                .build());
    }

    @Transactional
    public void deixarDeSeguir(Long followerId, Long followedId) {
        followerRepository.deleteByFollowerIdAndFollowedId(followerId, followedId);
    }

    public boolean verificarSeguindo(Long followerId, Long followedId) {
        return followerRepository.existsByFollowerIdAndFollowedId(followerId, followedId);
    }

    public long contarSeguidores(Long userId) {
        return followerRepository.countByFollowedId(userId);
    }

    public long contarSeguindo(Long userId) {
        return followerRepository.countByFollowerId(userId);
    }

    public List<FollowUserDTO> listarSeguidores(Long userId, Long currentUserId) {
        return followerRepository.findByFollowedId(userId)
                .stream()
                .map(f -> buildFollowUserDTO(f.getFollower(), currentUserId))
                .toList();
    }

    public List<FollowUserDTO> listarSeguindo(Long userId, Long currentUserId) {
        return followerRepository.findByFollowerId(userId)
                .stream()
                .map(f -> buildFollowUserDTO(f.getFollowed(), currentUserId))
                .toList();
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));
    }

    private void validateNaoSegueASiMesmo(Long followerId, Long followedId) {
        if (followerId.equals(followedId)) {
            throw new SelfFollowException();
        }
    }

    private boolean jaSegue(Long followerId, Long followedId) {
        return followerRepository.existsByFollowerIdAndFollowedId(followerId, followedId);
    }

    private FollowUserDTO buildFollowUserDTO(User user, Long currentUserId) {
        FollowUserDTO dto = new FollowUserDTO();
        dto.setUserId(user.getId());
        dto.setName(user.getName());
        dto.setReviewCount(reviewRepository.findByUserId(user.getId()).size());
        dto.setFollowing(currentUserId != null &&
                followerRepository.existsByFollowerIdAndFollowedId(currentUserId, user.getId()));
        return dto;
    }
}
