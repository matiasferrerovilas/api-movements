-- liquibase formatted sql
-- changeset mati:20260323-rename-services-to-subscription
ALTER TABLE services RENAME TO subscription;
ALTER TABLE subscription RENAME INDEX FK_SERVICES_ON_ACCOUNT TO FK_SUBSCRIPTION_ON_ACCOUNT;
