-- liquibase formatted sql
-- changeset mati:1766349485363-1

ALTER TABLE movements
    DROP COLUMN year,
    DROP COLUMN month;