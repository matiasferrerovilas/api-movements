-- liquibase formatted sql

-- changeset mati:1766349485363-1
ALTER TABLE ingreso
    DROP FOREIGN KEY FK_INGRESO_ON_USER_GROUP;

ALTER TABLE ingreso
    CHANGE COLUMN user_group_id account_id BIGINT NULL;

ALTER TABLE ingreso
    ADD CONSTRAINT FK_INGRESO_ON_ACCOUNT
        FOREIGN KEY (account_id)
            REFERENCES accounts(id);