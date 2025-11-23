--liquibase formatted sql
--changeset splitStatements:true

DROP TABLE IF EXISTS budget;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS movements;
DROP TABLE IF EXISTS currency;
DROP TABLE IF EXISTS services;
DROP TABLE IF EXISTS user_groups;

CREATE TABLE budget
(
    id          BIGINT AUTO_INCREMENT NOT NULL,
    user        VARCHAR(255) NOT NULL,
    category_id BIGINT NULL,
    amount      DECIMAL NULL,
    year        INT NULL,
    month       INT NULL,
    CONSTRAINT pk_budget PRIMARY KEY (id)
);

-- changeset mati:1761664291431-2
CREATE TABLE category
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    `description` VARCHAR(50) NOT NULL,
    CONSTRAINT pk_category PRIMARY KEY (id)
);

-- changeset mati:1761664291431-3
CREATE TABLE currency
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    `description` VARCHAR(20) NOT NULL,
    symbol        VARCHAR(5)  NOT NULL,
    CONSTRAINT pk_currency PRIMARY KEY (id)
);

-- changeset mati:1761664291431-4
CREATE TABLE movements
(
    id             BIGINT AUTO_INCREMENT NOT NULL,
    amount         DECIMAL(15, 2) NOT NULL,
    `description`  VARCHAR(60)    NOT NULL,
    date           date           NOT NULL,
    created_at     datetime NULL,
    updated_at     datetime NULL,
    category_id    BIGINT NULL,
    currency_id    BIGINT NULL,
    user_id        BIGINT NULL,
    year           INT            NOT NULL,
    month          INT            NOT NULL,
    bank           VARCHAR(30) NULL,
    user_group_id  BIGINT NULL,
    movement_type  VARCHAR(20) NULL,
    cuota_actual   INT             NULL,
    cuotas_totales INT             NULL,
    CONSTRAINT pk_movements PRIMARY KEY (id)
);

-- changeset mati:1761664291431-5
CREATE TABLE services
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    `description` VARCHAR(50) NOT NULL,
    last_payment  date NULL,
    amount        DECIMAL NULL,
    currency_id   BIGINT NULL,
    CONSTRAINT pk_services PRIMARY KEY (id)
);

-- changeset mati:1761664291431-6
CREATE TABLE user_groups
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    `description` VARCHAR(255) NULL,
    CONSTRAINT pk_usergroups PRIMARY KEY (id)
);

-- changeset mati:1761664291431-7
CREATE TABLE user_user_groups
(
    group_id BIGINT NOT NULL,
    user_id  BIGINT NOT NULL
);

-- changeset mati:1761664291431-8
CREATE TABLE users
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    email      VARCHAR(255) NULL,
    created_at datetime NULL,
    updated_at datetime NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);

-- changeset mati:1761664291431-9
ALTER TABLE category
    ADD CONSTRAINT uc_category_description UNIQUE (`description`);

-- changeset mati:1761664291431-10
ALTER TABLE services
    ADD CONSTRAINT uc_services_description UNIQUE (`description`);

-- changeset mati:1761664291431-11
ALTER TABLE budget
    ADD CONSTRAINT FK_BUDGET_ON_CATEGORY FOREIGN KEY (category_id) REFERENCES category (id);

-- changeset mati:1761664291431-12
ALTER TABLE movements
    ADD CONSTRAINT FK_MOVEMENTS_ON_CATEGORY FOREIGN KEY (category_id) REFERENCES category (id);

-- changeset mati:1761664291431-13
ALTER TABLE movements
    ADD CONSTRAINT FK_MOVEMENTS_ON_CURRENCY FOREIGN KEY (currency_id) REFERENCES currency (id);

-- changeset mati:1761664291431-14
ALTER TABLE movements
    ADD CONSTRAINT FK_MOVEMENTS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

-- changeset mati:1761664291431-15
ALTER TABLE movements
    ADD CONSTRAINT FK_MOVEMENTS_ON_USER_GROUP FOREIGN KEY (user_group_id) REFERENCES user_groups (id);

-- changeset mati:1761664291431-16
ALTER TABLE services
    ADD CONSTRAINT FK_SERVICES_ON_CURRENCY FOREIGN KEY (currency_id) REFERENCES currency (id);

-- changeset mati:1761664291431-17
ALTER TABLE user_user_groups
    ADD CONSTRAINT fk_useusegro_on_user FOREIGN KEY (user_id) REFERENCES users (id);

-- changeset mati:1761664291431-18
ALTER TABLE user_user_groups
    ADD CONSTRAINT fk_useusegro_on_user_groups FOREIGN KEY (group_id) REFERENCES user_groups (id);

INSERT INTO currency(symbol, description) VALUES ("ARS", "Peso Argentino"),
                                                 ("USD", "Dolar Americano"),
                                                 ("EUR", "Euro"),
                                                 ("CHF", "Franco Suizo");
INSERT INTO category (description) VALUES
                                       ('Sin Categoria'),
                                       ('Hogar'),
                                       ('Regalos'),
                                       ('Restaurante'),
                                       ('Ropa'),
                                       ('Servicios'),
                                       ('Streaming'),
                                       ('Supermercado'),
                                       ('Tecnologia'),
                                       ('Transporte'),
                                       ('Viaje');