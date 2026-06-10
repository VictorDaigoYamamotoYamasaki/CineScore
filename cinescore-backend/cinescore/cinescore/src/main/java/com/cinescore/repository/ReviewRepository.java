package com.cinescore.repository;

import com.cinescore.model.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, String> {

    List<Review> findAllByOrderByCreatedAtDesc();

    List<Review> findByMovieIdOrderByCreatedAtDesc(String movieId);

    List<Review> findByUserIdOrderByCreatedAtDesc(String userId);

    long countByUserId(String userId);

    void deleteByUserId(String userId);

    @Query("SELECT r.movieId FROM Review r GROUP BY r.movieId ORDER BY COUNT(r) DESC")
    List<String> findMostReviewedMovies(Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.movieId = :movieId AND r.movieTitle IS NOT NULL ORDER BY r.createdAt DESC")
    List<Review> findTopByMovieIdWithTitle(String movieId, Pageable pageable);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.movieId = :movieId")
    long countByMovieId(String movieId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.movieId = :movieId")
    Double avgRatingByMovieId(String movieId);

    @Query("SELECT r FROM Review r WHERE r.createdAt >= :since ORDER BY r.createdAt ASC")
    List<Review> findRecentReviews(@Param("since") LocalDateTime since);

    @Query("SELECT r.rating, COUNT(r) FROM Review r GROUP BY r.rating ORDER BY r.rating")
    List<Object[]> findRatingCounts();

    @Query("SELECT r.movieId FROM Review r GROUP BY r.movieId ORDER BY AVG(r.rating) DESC")
    List<String> findHighestRatedMovieId(Pageable pageable);

    @Query("SELECT r.movieId FROM Review r GROUP BY r.movieId ORDER BY AVG(r.rating) ASC")
    List<String> findLowestRatedMovieId(Pageable pageable);

    List<Review> findByUserIdOrderByWatchedAtDescIdDesc(String userId);
}
