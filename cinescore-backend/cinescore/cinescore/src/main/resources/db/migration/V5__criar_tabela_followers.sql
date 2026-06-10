CREATE TABLE followers (
    id               CHAR(36) NOT NULL DEFAULT (UUID()),
    follower_user_id CHAR(36) NOT NULL,
    followed_user_id CHAR(36) NOT NULL,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_followers          PRIMARY KEY (id),
    CONSTRAINT fk_followers_follower FOREIGN KEY (follower_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_followers_followed FOREIGN KEY (followed_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_followers          UNIQUE (follower_user_id, followed_user_id)
);

CREATE INDEX idx_followers_follower_id ON followers (follower_user_id);
CREATE INDEX idx_followers_followed_id ON followers (followed_user_id);
