--liquibase formatted sql

--changeset mati:061-create-table-movement-items
CREATE TABLE movement_items
(
    id          BIGINT AUTO_INCREMENT NOT NULL,
    movement_id BIGINT                NOT NULL,
    quantity    DECIMAL(15, 3)        NOT NULL,
    unit        VARCHAR(20)           NOT NULL,
    description VARCHAR(120)          NOT NULL,
    price       DECIMAL(15, 2)        NOT NULL,
    created_at  DATETIME              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_movement_items PRIMARY KEY (id),
    CONSTRAINT fk_movement_items_movement FOREIGN KEY (movement_id) REFERENCES movements (id)
);

--changeset mati:061-index-movement-items-movement
CREATE INDEX idx_movement_item_movement ON movement_items (movement_id);
