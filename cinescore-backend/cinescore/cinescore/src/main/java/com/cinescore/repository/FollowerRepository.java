package com.cinescore.repository;

import com.cinescore.model.Follower;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowerRepository extends JpaRepository<Follower, String> {
    List<Follower> findByFollowedId(String followedId);
    List<Follower> findByFollowerId(String followerId);
    Optional<Follower> findByFollowerIdAndFollowedId(String followerId, String followedId);
    boolean existsByFollowerIdAndFollowedId(String followerId, String followedId);
    long countByFollowedId(String followedId);
    long countByFollowerId(String followerId);
    void deleteByFollowerIdAndFollowedId(String followerId, String followedId);
}
