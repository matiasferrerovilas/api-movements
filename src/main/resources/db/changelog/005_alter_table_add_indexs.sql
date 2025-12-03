-- liquibase formatted sql
ALTER TABLE services
    DROP INDEX uc_services_description;

ALTER TABLE services
    ADD CONSTRAINT uc_services_user_desc UNIQUE (user_id, description);


ALTER TABLE user_groups
    MODIFY `description` VARCHAR(255) NOT NULL;


-- changeset mati:1764069382038-1
ALTER TABLE users
    ADD is_first_login BIT(1) NOT NULL;

-- changeset mati:1764069382038-3
ALTER TABLE user_user_groups
    ADD PRIMARY KEY (group_id, user_id);


