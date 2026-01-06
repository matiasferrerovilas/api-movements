-- liquibase formatted sql
-- changeset mati:1766349485363-1

ALTER TABLE currency
    ADD CONSTRAINT uk_currency_symbol UNIQUE (symbol);
