-- liquibase formatted sql
ALTER TABLE services
    DROP INDEX uc_services_description;

ALTER TABLE services
    ADD CONSTRAINT uc_services_user_desc UNIQUE (user_id, description);



