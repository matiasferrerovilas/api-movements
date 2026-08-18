-- liquibase formatted sql

-- changeset mati:054-create-table-goal
CREATE TABLE goal
(
    id             BIGINT AUTO_INCREMENT NOT NULL,
    workspace_id   BIGINT                NOT NULL,
    name           VARCHAR(150)          NOT NULL,
    target_amount  DECIMAL(15, 2)        NOT NULL,
    current_amount DECIMAL(15, 2)        NOT NULL DEFAULT 0,
    currency_id    BIGINT                NOT NULL,
    target_date    DATE                  NULL,
    created_at     DATETIME              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_goal PRIMARY KEY (id),
    CONSTRAINT fk_goal_currency FOREIGN KEY (currency_id) REFERENCES currency (id)
);

-- changeset mati:054-index-goal-workspace
CREATE INDEX idx_goal_workspace ON goal (workspace_id);
