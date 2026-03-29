-- liquibase formatted sql
-- changeset mgferrero:027-drop-bank-column-from-movements

ALTER TABLE movements DROP COLUMN bank;
