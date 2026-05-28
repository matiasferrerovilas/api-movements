--liquibase formatted sql

--changeset mati:044-create-investment-type-1
CREATE TABLE investment_type
(
    id           BIGINT AUTO_INCREMENT NOT NULL,
    name         VARCHAR(50)           NOT NULL,
    icon_name    VARCHAR(50)           NOT NULL DEFAULT 'QuestionOutlined',
    icon_color   VARCHAR(20)           NOT NULL DEFAULT '#d9d9d9',
    workspace_id BIGINT                NOT NULL,
    created_at   DATETIME              NULL,
    CONSTRAINT pk_investment_type PRIMARY KEY (id)
);

--changeset mati:044-create-investment-type-2
CREATE INDEX idx_investment_type_workspace ON investment_type (workspace_id);

--changeset mati:044-create-investment-type-3
ALTER TABLE investment_type
    ADD CONSTRAINT fk_investment_type_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces (id);
