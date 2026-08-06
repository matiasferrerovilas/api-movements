--liquibase formatted sql

--changeset mati:051-create-workspace-currencies
CREATE TABLE workspace_currencies
(
    id           BIGINT AUTO_INCREMENT NOT NULL,
    workspace_id BIGINT                NOT NULL,
    currency_id  BIGINT                NOT NULL,
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_workspace_currencies PRIMARY KEY (id),
    CONSTRAINT fk_workspace_currencies_currency FOREIGN KEY (currency_id) REFERENCES currency (id),
    CONSTRAINT uq_workspace_currencies UNIQUE (workspace_id, currency_id)
);

--changeset mati:051-index-workspace-currencies
CREATE INDEX idx_workspace_currencies_workspace ON workspace_currencies (workspace_id);

--changeset mati:051-backfill-default-currencies-for-existing-workspaces
INSERT IGNORE INTO workspace_currencies (workspace_id, currency_id)
SELECT DISTINCT w.workspace_id, c.id
FROM (
    SELECT workspace_id FROM movements
    UNION SELECT workspace_id FROM ingreso
    UNION SELECT workspace_id FROM subscription
    UNION SELECT workspace_id FROM budget
) w
CROSS JOIN currency c
WHERE c.enabled = TRUE;

--changeset mati:051-backfill-used-currencies-for-existing-workspaces
INSERT IGNORE INTO workspace_currencies (workspace_id, currency_id)
SELECT DISTINCT workspace_id, currency_id FROM movements WHERE currency_id IS NOT NULL
UNION
SELECT DISTINCT workspace_id, currency_id FROM ingreso WHERE currency_id IS NOT NULL
UNION
SELECT DISTINCT workspace_id, currency_id FROM subscription WHERE currency_id IS NOT NULL
UNION
SELECT DISTINCT workspace_id, currency_id FROM budget WHERE currency_id IS NOT NULL;
