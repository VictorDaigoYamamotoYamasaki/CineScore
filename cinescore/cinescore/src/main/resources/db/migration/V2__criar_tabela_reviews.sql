CREATE TABLE reviews (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    movie_imdb_id VARCHAR(20)  NOT NULL,
    rating        TINYINT      NOT NULL,
    review_text   TEXT,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_reviews PRIMARY KEY (id),
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,

    -- Um usuário só pode ter uma review por filme
    CONSTRAINT uq_reviews_user_movie UNIQUE (user_id, movie_imdb_id),

    -- Nota deve ser entre 1 e 5
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);
