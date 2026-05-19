package com.cinescore.repository;

import com.cinescore.model.Follower;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowerRepository extends JpaRepository<Follower, Long> {
    List<Follower> findByFollowedId(Long followedId);
    List<Follower> findByFollowerId(Long followerId);
    Optional<Follower> findByFollowerIdAndFollowedId(Long followerId, Long followedId);
    boolean existsByFollowerIdAndFollowedId(Long followerId, Long followedId);
    long countByFollowedId(Long followedId);
    long countByFollowerId(Long followerId);
    void deleteByFollowerIdAndFollowedId(Long followerId, Long followedId);
}
