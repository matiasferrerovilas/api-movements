-- liquibase formatted sql
-- changeset mferrero:037_alter_table_subscription_add_timestamps

ALTER TABLE subscription 
    ADD COLUMN created_at DATETIME NULL,
    ADD COLUMN updated_at DATETIME NULL;
