--liquibase formatted sql

--changeset mati:035-recreate-budget-1
DROP TABLE IF EXISTS budget;

--changeset mati:035-recreate-budget-2
CREATE TABLE budget
(
    id          BIGINT AUTO_INCREMENT NOT NULL,
    account_id  BIGINT       NOT NULL,
    category_id BIGINT       NULL,
    currency_id BIGINT       NOT NULL,
    amount      DECIMAL(15, 2) NOT NULL,
    year        INT          NULL,
    month       INT          NULL,
    CONSTRAINT pk_budget PRIMARY KEY (id),
    CONSTRAINT uq_budget_account_category_currency UNIQUE (account_id, category_id, currency_id)
);

--changeset mati:035-recreate-budget-3
ALTER TABLE budget
    ADD CONSTRAINT fk_budget_account FOREIGN KEY (account_id) REFERENCES accounts (id);

--changeset mati:035-recreate-budget-4
ALTER TABLE budget
    ADD CONSTRAINT fk_budget_category FOREIGN KEY (category_id) REFERENCES category (id);

--changeset mati:035-recreate-budget-5
ALTER TABLE budget
    ADD CONSTRAINT fk_budget_currency FOREIGN KEY (currency_id) REFERENCES currency (id);
