-- liquibase formatted sql

-- changeset mati:1763940426915-1
ALTER TABLE user_groups
    MODIFY `description` VARCHAR(255) NOT NULL;

