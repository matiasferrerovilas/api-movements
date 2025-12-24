-- liquibase formatted sql

-- changeset mati:1766349485363-1
CREATE TABLE account_members
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    account_id BIGINT                NOT NULL,
    user_id    BIGINT                NOT NULL,
    `role`     VARCHAR(255)          NOT NULL,
    joined_at  datetime              NULL,
    CONSTRAINT pk_account_members PRIMARY KEY (id)
);

-- changeset mati:1766349485363-2
CREATE TABLE accounts
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    name       VARCHAR(255)          NOT NULL,
    owner_id   BIGINT                NOT NULL,
    created_at datetime              NULL,
    CONSTRAINT pk_accounts PRIMARY KEY (id)
);

-- changeset mati:1766349485363-3
ALTER TABLE account_members
    ADD CONSTRAINT uc_2604de3a574bddd99b13649a5 UNIQUE (account_id, user_id);

-- changeset mati:1766349485363-4
ALTER TABLE accounts
    ADD CONSTRAINT uc_4678018778e6363f218cfd3ce UNIQUE (owner_id, name);

-- changeset mati:1766349485363-5
ALTER TABLE accounts
    ADD CONSTRAINT FK_ACCOUNTS_ON_OWNER FOREIGN KEY (owner_id) REFERENCES users (id);

-- changeset mati:1766349485363-6
ALTER TABLE account_members
    ADD CONSTRAINT FK_ACCOUNT_MEMBERS_ON_ACCOUNT FOREIGN KEY (account_id) REFERENCES accounts (id);

-- changeset mati:1766349485363-7
ALTER TABLE account_members
    ADD CONSTRAINT FK_ACCOUNT_MEMBERS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

