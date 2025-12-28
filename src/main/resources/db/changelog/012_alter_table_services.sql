-- liquibase formatted sql

-- changeset mati:1766349485363-1
ALTER TABLE services
    DROP FOREIGN KEY FK_SERVICES_ON_USER_GROUP;

ALTER TABLE services
    CHANGE COLUMN user_group_id account_id BIGINT NULL;

ALTER TABLE services
    ADD CONSTRAINT FK_SERVICES_ON_ACCOUNT
        FOREIGN KEY (account_id)
            REFERENCES accounts(id);