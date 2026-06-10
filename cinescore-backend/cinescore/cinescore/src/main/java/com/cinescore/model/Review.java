package com.cinescore.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Review {

    @Id
    @UuidGenerator
    @Column(length = 36, updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "movie_id", nullable = false, length = 50)
    private String movieId;

    @Column(name = "movie_title", length = 255)
    private String movieTitle;

    @Column(name = "movie_poster", length = 500)
    private String moviePoster;

    @DecimalMin("0.5") @DecimalMax("5.0")
    @Column(nullable = false)
    private Double rating;

    @Column(name = "watched_at", nullable = false)
    private LocalDate watchedAt;

    @Column(name = "review_text", columnDefinition = "TEXT")
    private String reviewText;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    private void inicializarCampos() {
        if (this.watchedAt == null) {
            this.watchedAt = LocalDate.now();
        }
    }
}
