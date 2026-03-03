-- liquibase formatted sql
-- changeset mati:019-add-is-default

ALTER TABLE account_members
    ADD COLUMN is_default BOOLEAN NOT NULL DEFAULT FALSE;
