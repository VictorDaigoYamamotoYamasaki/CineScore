package com.cinescore.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "movies_cache")
@Getter @Setter @ToString @Builder @NoArgsConstructor @AllArgsConstructor
public class MovieCache {

    @Id
    @Column(name = "movie_id", length = 20)
    private String movieId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "year", length = 4)
    private String year;

    @Column(name = "poster", length = 500)
    private String poster;

    @Column(name = "synopsis", columnDefinition = "TEXT")
    private String synopsis;

    @Column(name = "genres", length = 500)
    private String genres;

    @Column(name = "director", length = 255)
    private String director;

    @Column(name = "actors", length = 500)
    private String actors;

    @Column(name = "runtime")
    private Integer runtime;

    @Column(name = "vote_average")
    private Double voteAverage;

    @Column(name = "certification", length = 10)
    private String certification;

    @Column(name = "cached_at", nullable = false)
    private LocalDateTime cachedAt;

    @PrePersist @PreUpdate
    private void atualizarTimestamp() {
        this.cachedAt = LocalDateTime.now();
    }
}
