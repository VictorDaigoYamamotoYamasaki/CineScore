package com.cinescore.repository;

import com.cinescore.model.ReviewHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewHistoryRepository extends JpaRepository<ReviewHistory, String> {
    List<ReviewHistory> findByReviewIdOrderByCreatedAtDesc(String reviewId);
}
