package com.cinescore.repository;

import com.cinescore.model.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface WatchlistRepository extends JpaRepository<WatchlistItem, String> {

    List<WatchlistItem> findByUserIdOrderByCreatedAtDesc(String userId);
    boolean existsByUserIdAndMovieId(String userId, String movieId);
    @Modifying
    @Transactional
    void deleteByUserIdAndMovieId(String userId, String movieId);
}
