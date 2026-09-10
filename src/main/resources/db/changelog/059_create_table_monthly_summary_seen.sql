-- liquibase formatted sql

-- changeset mati:059-create-table-monthly-summary-seen
CREATE TABLE monthly_summary_seen
(
    id           BIGINT AUTO_INCREMENT NOT NULL,
    workspace_id BIGINT                NOT NULL,
    user_id      BIGINT                NOT NULL,
    year         INT                   NOT NULL,
    month        INT                   NOT NULL,
    seen_at      DATETIME              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_monthly_summary_seen PRIMARY KEY (id),
    CONSTRAINT uq_monthly_summary_seen_workspace_user_period UNIQUE (workspace_id, user_id, year, month)
);

-- changeset mati:059-index-monthly-summary-seen-workspace
CREATE INDEX idx_monthly_summary_seen_workspace ON monthly_summary_seen (workspace_id);
