package com.cinescore.repository;

import com.cinescore.model.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    List<Reaction> findByReviewId(Long reviewId);
    Optional<Reaction> findByReviewIdAndUserIdAndEmoji(Long reviewId, Long userId, String emoji);
}
