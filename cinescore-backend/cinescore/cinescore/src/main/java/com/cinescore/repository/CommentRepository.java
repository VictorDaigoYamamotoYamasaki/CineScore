package com.cinescore.repository;

import com.cinescore.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, String> {
    List<Comment> findByReviewIdOrderByCreatedAtAsc(String reviewId);
    long countByReviewId(String reviewId);
}
