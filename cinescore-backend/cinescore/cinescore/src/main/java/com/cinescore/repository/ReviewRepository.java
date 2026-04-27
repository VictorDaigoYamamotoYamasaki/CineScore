package com.cinescore.repository;

import com.cinescore.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByMovieImdbId(String movieImdbId);

    List<Review> findByUserId(Long userId);
}
