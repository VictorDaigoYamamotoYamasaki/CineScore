CREATE TABLE followers (
    id               BIGINT   NOT NULL AUTO_INCREMENT,
    follower_user_id BIGINT   NOT NULL,
    followed_user_id BIGINT   NOT NULL,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_followers PRIMARY KEY (id),
    CONSTRAINT fk_followers_follower FOREIGN KEY (follower_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_followers_followed FOREIGN KEY (followed_user_id) REFERENCES users (id) ON DELETE CASCADE,

    -- Um usuário não pode seguir a mesma pessoa duas vezes
    CONSTRAINT uq_followers_pair UNIQUE (follower_user_id, followed_user_id),

    -- Um usuário não pode seguir a si mesmo
    CONSTRAINT chk_followers_self CHECK (follower_user_id <> followed_user_id)
);
