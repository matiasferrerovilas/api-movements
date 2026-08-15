-- liquibase formatted sql

-- changeset mati:053_alter_table_subscription_add_overdue_notified
ALTER TABLE subscription
    ADD COLUMN overdue_notified_year INT NULL,
    ADD COLUMN overdue_notified_month INT NULL;
