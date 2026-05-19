CREATE TABLE favorite_movies (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    movie_id   VARCHAR(50) NOT NULL,
    position   INT         NOT NULL,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_favorite_movies          PRIMARY KEY (id),
    CONSTRAINT fk_favorite_movies_user     FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_favorite_user_position   UNIQUE (user_id, position),
    CONSTRAINT uq_favorite_user_movie      UNIQUE (user_id, movie_id),
    CONSTRAINT chk_favorite_position       CHECK (position BETWEEN 1 AND 5)
);
