--liquibase formatted sql

--changeset mati:063-create-table-category-rule
-- Reglas aprendidas: comercio normalizado -> categoría, por workspace. Se crean o actualizan cada
-- vez que el usuario corrige la categoría de un movimiento y se usan al importar extractos.
CREATE TABLE category_rule
(
    id           BIGINT AUTO_INCREMENT NOT NULL,
    workspace_id BIGINT                NOT NULL,
    merchant_key VARCHAR(60)           NOT NULL,
    category_id  BIGINT                NOT NULL,
    hits         INT                   NOT NULL DEFAULT 1,
    created_at   DATETIME              NULL,
    updated_at   DATETIME              NULL,
    CONSTRAINT pk_category_rule PRIMARY KEY (id),
    CONSTRAINT uc_category_rule_workspace_key UNIQUE (workspace_id, merchant_key),
    CONSTRAINT fk_category_rule_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE CASCADE
);
