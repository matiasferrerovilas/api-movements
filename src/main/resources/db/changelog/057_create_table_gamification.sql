-- liquibase formatted sql

-- changeset mati:057-create-table-user-streaks
CREATE TABLE user_streaks
(
    id                 BIGINT AUTO_INCREMENT NOT NULL,
    user_id            BIGINT                NOT NULL,
    workspace_id       BIGINT                NOT NULL,
    current_streak     INT                   NOT NULL DEFAULT 0,
    longest_streak     INT                   NOT NULL DEFAULT 0,
    last_activity_date DATE                  NULL,
    CONSTRAINT pk_user_streaks PRIMARY KEY (id),
    CONSTRAINT uq_user_streaks_user_workspace UNIQUE (user_id, workspace_id)
);

-- changeset mati:057-create-table-badges
CREATE TABLE badges
(
    id           BIGINT AUTO_INCREMENT NOT NULL,
    workspace_id BIGINT                NOT NULL,
    category_id  BIGINT                NULL,
    type         VARCHAR(30)           NOT NULL,
    year         INT                   NOT NULL,
    month        INT                   NOT NULL,
    earned_at    DATETIME              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_badges PRIMARY KEY (id),
    CONSTRAINT fk_badges_category FOREIGN KEY (category_id) REFERENCES category (id),
    CONSTRAINT uq_badges_workspace_category_period UNIQUE (workspace_id, category_id, year, month, type)
);

-- changeset mati:057-index-badges-workspace
CREATE INDEX idx_badges_workspace ON badges (workspace_id);
