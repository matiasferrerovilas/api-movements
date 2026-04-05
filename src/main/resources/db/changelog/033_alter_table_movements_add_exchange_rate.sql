-- liquibase formatted sql
-- changeset mferrero:033

ALTER TABLE movements ADD COLUMN exchange_rate DECIMAL(15, 6) NULL;
