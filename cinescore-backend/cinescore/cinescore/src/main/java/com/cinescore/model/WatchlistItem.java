package com.cinescore.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "watchlist")
@Getter @Setter @ToString(exclude = "user")
@Builder @NoArgsConstructor @AllArgsConstructor
public class WatchlistItem {

    @Id
    @org.hibernate.annotations.UuidGenerator
    @Column(length = 36, updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "movie_id", nullable = false, length = 20)
    private String movieId;

    @Column(name = "movie_title", length = 255)
    private String movieTitle;

    @Column(name = "movie_poster", length = 500)
    private String moviePoster;

    @Column(name = "movie_year", length = 4)
    private String movieYear;

    @Column(name = "movie_vote_average")
    private Double movieVoteAverage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
