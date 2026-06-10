CREATE TABLE movies_cache (
    movie_id      VARCHAR(20)   NOT NULL,
    title         VARCHAR(255)  NOT NULL,
    year          VARCHAR(4)    NULL,
    poster        VARCHAR(500)  NULL,
    synopsis      TEXT          NULL,
    genres        VARCHAR(500)  NULL,
    director      VARCHAR(255)  NULL,
    actors        VARCHAR(500)  NULL,
    runtime       INT           NULL,
    vote_average  DECIMAL(3,1)  NULL,
    certification VARCHAR(10)   NULL,
    cached_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_movies_cache PRIMARY KEY (movie_id)
);
