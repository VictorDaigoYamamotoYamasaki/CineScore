package com.cinescore.repository;

import com.cinescore.model.FavoriteMovie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteMovieRepository extends JpaRepository<FavoriteMovie, String> {
    List<FavoriteMovie> findByUserIdOrderByPosition(String userId);
    Optional<FavoriteMovie> findByUserIdAndPosition(String userId, Integer position);
    void deleteByUserIdAndPosition(String userId, Integer position);
}
