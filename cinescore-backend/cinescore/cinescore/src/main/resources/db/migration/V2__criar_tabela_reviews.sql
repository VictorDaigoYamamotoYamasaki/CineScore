CREATE TABLE reviews (
    id           CHAR(36)     NOT NULL DEFAULT (UUID()),
    user_id      CHAR(36)     NOT NULL,
    movie_id     VARCHAR(20)  NOT NULL,
    movie_title  VARCHAR(255) NULL,
    movie_poster VARCHAR(500) NULL,
    rating       DECIMAL(2,1) NOT NULL,
    review_text  TEXT         NULL,
    watched_at   DATE         NOT NULL DEFAULT (CURRENT_DATE),
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_reviews         PRIMARY KEY (id),
    CONSTRAINT fk_reviews_user    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 0.5 AND 5.0)
);

CREATE INDEX idx_reviews_user_id  ON reviews (user_id);
CREATE INDEX idx_reviews_movie_id ON reviews (movie_id);
