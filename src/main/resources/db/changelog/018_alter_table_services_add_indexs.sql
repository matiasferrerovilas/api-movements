-- liquibase formatted sql
ALTER TABLE services
    DROP INDEX uc_services_user_desc;

ALTER TABLE services
    ADD CONSTRAINT uc_services_user_currency_desc UNIQUE (user_id, currency_id, description);

