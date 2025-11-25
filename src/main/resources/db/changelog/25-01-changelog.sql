-- liquibase formatted sql

-- changeset mati:1764069382038-1
ALTER TABLE users
    ADD is_first_login BIT(1) NOT NULL;

-- changeset mati:1764069382038-3
ALTER TABLE user_user_groups
    ADD PRIMARY KEY (group_id, user_id);

