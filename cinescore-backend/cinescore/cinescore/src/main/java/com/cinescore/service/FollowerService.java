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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowerService {

    private final FollowerRepository followerRepository;
    private final UserRepository     userRepository;
    private final ReviewRepository   reviewRepository;

    @Transactional
    public void seguir(String followerId, String followedId) {
        validateNaoSegueASiMesmo(followerId, followedId);
        if (jaSegue(followerId, followedId)) return;
        User follower = findUserOrThrow(followerId);
        User followed = findUserOrThrow(followedId);
        followerRepository.save(Follower.builder().follower(follower).followed(followed).build());
        log.info("Seguindo: followerId={} followedId={}", followerId, followedId);
    }

    @Transactional
    public void deixarDeSeguir(String followerId, String followedId) {
        followerRepository.deleteByFollowerIdAndFollowedId(followerId, followedId);
        log.info("Deixou de seguir: followerId={} followedId={}", followerId, followedId);
    }

    @Transactional(readOnly = true)
    public boolean verificarSeguindo(String followerId, String followedId) {
        return followerRepository.existsByFollowerIdAndFollowedId(followerId, followedId);
    }

    @Transactional(readOnly = true)
    public long contarSeguidores(String userId) {
        return followerRepository.countByFollowedId(userId);
    }

    @Transactional(readOnly = true)
    public long contarSeguindo(String userId) {
        return followerRepository.countByFollowerId(userId);
    }

    @Transactional(readOnly = true)
    public List<FollowUserDTO> listarSeguidores(String userId, String currentUserId) {
        return followerRepository.findByFollowedId(userId).stream()
                .map(f -> buildFollowUserDTO(f.getFollower(), currentUserId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FollowUserDTO> listarSeguindo(String userId, String currentUserId) {
        return followerRepository.findByFollowerId(userId).stream()
                .map(f -> buildFollowUserDTO(f.getFollowed(), currentUserId))
                .toList();
    }


    private User findUserOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));
    }

    private void validateNaoSegueASiMesmo(String followerId, String followedId) {
        if (followerId.equals(followedId)) throw new SelfFollowException();
    }

    private boolean jaSegue(String followerId, String followedId) {
        return followerRepository.existsByFollowerIdAndFollowedId(followerId, followedId);
    }

    private FollowUserDTO buildFollowUserDTO(User user, String currentUserId) {
        FollowUserDTO dto = new FollowUserDTO();
        dto.setUserId(user.getId());
        dto.setName(user.getName());
        dto.setReviewCount(reviewRepository.countByUserId(user.getId()));
        dto.setFollowing(currentUserId != null &&
                followerRepository.existsByFollowerIdAndFollowedId(currentUserId, user.getId()));
        return dto;
    }
}
