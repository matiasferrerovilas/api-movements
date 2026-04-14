-- liquibase formatted sql
-- changeset mferrero:038_alter_table_users_add_has_seen_tour

ALTER TABLE users ADD COLUMN has_seen_tour BOOLEAN NOT NULL DEFAULT FALSE;
