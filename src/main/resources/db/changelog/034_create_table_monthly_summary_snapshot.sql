-- changeset mgferrero:034
CREATE TABLE monthly_summary_snapshot
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    year       INT          NOT NULL,
    month      INT          NOT NULL,
    payload    TEXT         NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT fk_snapshot_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_snapshot_user_year_month UNIQUE (user_id, year, month)
);
