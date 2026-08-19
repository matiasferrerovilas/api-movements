-- liquibase formatted sql
-- changeset mati:056_alter_table_currency_add_is_default

ALTER TABLE currency
    ADD COLUMN is_default BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE currency SET is_default = TRUE WHERE symbol = 'USD';
