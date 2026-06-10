package com.cinescore.repository;

import com.cinescore.model.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, String> {

    List<Reaction> findByReviewId(String reviewId);

    Optional<Reaction> findByReviewIdAndUserIdAndEmoji(String reviewId, String userId, String emoji);

    @Modifying
    @Query("DELETE FROM Reaction r WHERE r.review.id = :reviewId AND r.user.id = :userId")
    void deleteByReviewIdAndUserId(String reviewId, String userId);
}
