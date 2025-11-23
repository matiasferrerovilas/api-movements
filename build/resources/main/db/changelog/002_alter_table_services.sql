-- liquibase formatted sql

-- changeset mati:1762637621551-1
ALTER TABLE services
    ADD user_group_id BIGINT NULL, ADD user_id BIGINT NULL;

-- changeset mati:1762637621551-3
ALTER TABLE services
    ADD CONSTRAINT FK_SERVICES_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

-- changeset mati:1762637621551-4
ALTER TABLE services
    ADD CONSTRAINT FK_SERVICES_ON_USER_GROUP FOREIGN KEY (user_group_id) REFERENCES user_groups (id);

