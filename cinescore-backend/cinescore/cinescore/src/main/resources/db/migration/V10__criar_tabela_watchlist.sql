CREATE TABLE watchlist (
    id                 CHAR(36)     NOT NULL DEFAULT (UUID()),
    user_id            CHAR(36)     NOT NULL,
    movie_id           VARCHAR(20)  NOT NULL,
    movie_title        VARCHAR(255) NULL,
    movie_poster       VARCHAR(500) NULL,
    movie_year         VARCHAR(4)   NULL,
    movie_vote_average DECIMAL(3,1) NULL,
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_watchlist            PRIMARY KEY (id),
    CONSTRAINT fk_watchlist_user       FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_watchlist_user_movie UNIQUE (user_id, movie_id)
);

CREATE INDEX idx_watchlist_user_id ON watchlist (user_id);
