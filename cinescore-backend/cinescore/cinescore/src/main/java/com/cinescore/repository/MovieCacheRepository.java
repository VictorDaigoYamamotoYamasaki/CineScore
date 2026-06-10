package com.cinescore.repository;

import com.cinescore.model.MovieCache;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieCacheRepository extends JpaRepository<MovieCache, String> {}
