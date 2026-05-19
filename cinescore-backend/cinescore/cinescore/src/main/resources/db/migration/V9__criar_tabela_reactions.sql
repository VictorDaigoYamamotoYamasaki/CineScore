CREATE TABLE reactions (
    id        BIGINT      NOT NULL AUTO_INCREMENT,
    review_id BIGINT      NOT NULL,
    user_id   BIGINT      NOT NULL,
    emoji     VARCHAR(10) NOT NULL,
    created_at DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_reactions        PRIMARY KEY (id),
    CONSTRAINT fk_reactions_review FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE CASCADE,
    CONSTRAINT fk_reactions_user   FOREIGN KEY (user_id)   REFERENCES users   (id) ON DELETE CASCADE,
    CONSTRAINT uq_reaction_user    UNIQUE (review_id, user_id, emoji)
);
