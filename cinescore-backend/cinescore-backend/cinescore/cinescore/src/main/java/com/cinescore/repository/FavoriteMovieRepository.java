package com.cinescore.repository;

import com.cinescore.model.FavoriteMovie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteMovieRepository extends JpaRepository<FavoriteMovie, Long> {
    List<FavoriteMovie> findByUserIdOrderByPosition(Long userId);
    Optional<FavoriteMovie> findByUserIdAndPosition(Long userId, Integer position);
    void deleteByUserIdAndPosition(Long userId, Integer position);
}
