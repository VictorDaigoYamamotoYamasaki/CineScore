ALTER TABLE reviews
    ADD COLUMN movie_title  VARCHAR(255) NULL AFTER movie_id,
    ADD COLUMN movie_poster VARCHAR(500) NULL AFTER movie_title;
