package com.cinescore.repository;

import com.cinescore.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByMovieImdbId(String movieImdbId);

    List<Review> findByUserId(Long userId);

    Optional<Review> findByUserIdAndMovieImdbId(Long userId, String movieImdbId);

    boolean existsByUserIdAndMovieImdbId(Long userId, String movieImdbId);
}
