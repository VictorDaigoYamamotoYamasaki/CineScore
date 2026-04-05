CREATE TABLE comments (
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    review_id    BIGINT   NOT NULL,
    user_id      BIGINT   NOT NULL,
    comment_text TEXT     NOT NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_comments PRIMARY KEY (id),
    CONSTRAINT fk_comments_review FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_user   FOREIGN KEY (user_id)   REFERENCES users (id)   ON DELETE CASCADE
);
