-- liquibase formatted sql

-- changeset mati:1766349485363-1
ALTER TABLE movements
    DROP FOREIGN KEY FK_MOVEMENTS_ON_USER_GROUP;

ALTER TABLE movements
    CHANGE COLUMN user_group_id account_id BIGINT NULL;

ALTER TABLE movements
    ADD CONSTRAINT FK_MOVEMENTS_ON_ACCOUNT
        FOREIGN KEY (account_id)
            REFERENCES accounts(id);