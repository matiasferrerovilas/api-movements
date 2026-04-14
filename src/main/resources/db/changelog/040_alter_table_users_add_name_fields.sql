--liquibase formatted sql
--changeset mferrero:040

ALTER TABLE users ADD COLUMN given_name VARCHAR(255);
ALTER TABLE users ADD COLUMN family_name VARCHAR(255);
