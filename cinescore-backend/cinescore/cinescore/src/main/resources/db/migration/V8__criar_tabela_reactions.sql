CREATE TABLE reactions (
    id         CHAR(36)    NOT NULL DEFAULT (UUID()),
    review_id  CHAR(36)    NOT NULL,
    user_id    CHAR(36)    NOT NULL,
    emoji      VARCHAR(10) NOT NULL,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_reactions        PRIMARY KEY (id),
    CONSTRAINT fk_reactions_review FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE CASCADE,
    CONSTRAINT fk_reactions_user   FOREIGN KEY (user_id)   REFERENCES users   (id) ON DELETE CASCADE,
    CONSTRAINT uq_reaction_user    UNIQUE (review_id, user_id, emoji)
);

CREATE INDEX idx_reactions_review_id ON reactions (review_id);
CREATE INDEX idx_reactions_user_id   ON reactions (user_id);
