-- liquibase formatted sql

-- changeset mati:1765333587265-1
CREATE TABLE ingreso
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    amount        DECIMAL(15, 2)        NOT NULL,
    currency_id   BIGINT                NULL,
    user_id       BIGINT                NULL,
    bank          VARCHAR(255)          NULL,
    user_group_id BIGINT                NULL,
    created_at    datetime              NULL,
    updated_at    datetime              NULL,
    CONSTRAINT pk_ingreso PRIMARY KEY (id)
);

-- changeset mati:1765333587265-2
ALTER TABLE ingreso
    ADD CONSTRAINT FK_INGRESO_ON_CURRENCY FOREIGN KEY (currency_id) REFERENCES currency (id);

-- changeset mati:1765333587265-3
ALTER TABLE ingreso
    ADD CONSTRAINT FK_INGRESO_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

-- changeset mati:1765333587265-4
ALTER TABLE ingreso
    ADD CONSTRAINT FK_INGRESO_ON_USER_GROUP FOREIGN KEY (user_group_id) REFERENCES user_groups (id);

