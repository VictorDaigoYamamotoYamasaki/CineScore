CREATE TABLE comments (
    id           CHAR(36) NOT NULL DEFAULT (UUID()),
    review_id    CHAR(36) NOT NULL,
    user_id      CHAR(36) NOT NULL,
    comment_text TEXT     NOT NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_comments        PRIMARY KEY (id),
    CONSTRAINT fk_comments_review FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_user   FOREIGN KEY (user_id)   REFERENCES users   (id) ON DELETE CASCADE
);

CREATE INDEX idx_comments_review_id ON comments (review_id);
CREATE INDEX idx_comments_user_id   ON comments (user_id);
