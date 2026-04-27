CREATE TABLE review_history (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    review_id  BIGINT       NOT NULL,
    rating_old DECIMAL(2,1) NOT NULL,
    rating_new DECIMAL(2,1) NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_review_history PRIMARY KEY (id),
    CONSTRAINT fk_review_history_review FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE CASCADE,
    CONSTRAINT chk_review_history_rating_old CHECK (rating_old BETWEEN 0.5 AND 5),
    CONSTRAINT chk_review_history_rating_new CHECK (rating_new BETWEEN 0.5 AND 5)
);
