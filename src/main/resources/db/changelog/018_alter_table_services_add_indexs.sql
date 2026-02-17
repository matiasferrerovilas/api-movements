-- liquibase formatted sql
-- changeset mati:018-add-unique-services

ALTER TABLE services
    DROP INDEX uc_services_description;

ALTER TABLE services
    ADD CONSTRAINT uc_services_user_currency_desc UNIQUE (user_id, currency_id, description);

