-- liquibase formatted sql

-- changeset mati:1764763029297-1
ALTER TABLE users
    ADD user_type      VARCHAR(50) NULL;

