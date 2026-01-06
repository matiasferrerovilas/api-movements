-- liquibase formatted sql
-- changeset mati:1766349485363-1

ALTER TABLE currency
    ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT FALSE;

