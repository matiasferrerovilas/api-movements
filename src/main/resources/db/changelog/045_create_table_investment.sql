--liquibase formatted sql

--changeset mati:045-create-investment-1
CREATE TABLE investment
(
    id                   BIGINT AUTO_INCREMENT NOT NULL,
    amount               DECIMAL(15, 2)        NOT NULL,
    start_date           DATE                  NOT NULL,
    end_date             DATE                  NULL,
    description          VARCHAR(100)          NULL,
    investment_type_id   BIGINT                NOT NULL,
    currency_id          BIGINT                NOT NULL,
    workspace_id         BIGINT                NOT NULL,
    owner_id             BIGINT                NULL,
    created_at           DATETIME              NULL,
    updated_at           DATETIME              NULL,
    CONSTRAINT pk_investment PRIMARY KEY (id)
);

--changeset mati:045-create-investment-2
CREATE INDEX idx_investment_workspace ON investment (workspace_id);

--changeset mati:045-create-investment-3
ALTER TABLE investment
    ADD CONSTRAINT fk_investment_type FOREIGN KEY (investment_type_id) REFERENCES investment_type (id);

--changeset mati:045-create-investment-4
ALTER TABLE investment
    ADD CONSTRAINT fk_investment_currency FOREIGN KEY (currency_id) REFERENCES currency (id);

--changeset mati:045-create-investment-5
ALTER TABLE investment
    ADD CONSTRAINT fk_investment_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces (id);

--changeset mati:045-create-investment-6
ALTER TABLE investment
    ADD CONSTRAINT fk_investment_owner FOREIGN KEY (owner_id) REFERENCES users (id);
