--liquibase formatted sql

--changeset mati:052-create-movement-categories
CREATE TABLE movement_categories
(
    id          BIGINT AUTO_INCREMENT NOT NULL,
    movement_id BIGINT                NOT NULL,
    category_id BIGINT                NOT NULL,
    CONSTRAINT pk_movement_categories PRIMARY KEY (id),
    CONSTRAINT fk_movement_categories_movement FOREIGN KEY (movement_id) REFERENCES movements (id),
    CONSTRAINT fk_movement_categories_category FOREIGN KEY (category_id) REFERENCES category (id),
    CONSTRAINT uq_movement_categories UNIQUE (movement_id, category_id)
);

--changeset mati:052-index-movement-categories-movement
CREATE INDEX idx_movement_categories_movement ON movement_categories (movement_id);

--changeset mati:052-index-movement-categories-category
CREATE INDEX idx_movement_categories_category ON movement_categories (category_id);

--changeset mati:052-backfill-movement-categories
INSERT INTO movement_categories (movement_id, category_id)
SELECT id, category_id FROM movements WHERE category_id IS NOT NULL;

--changeset mati:052-drop-old-category-fk
ALTER TABLE movements DROP FOREIGN KEY FK_MOVEMENTS_ON_CATEGORY;

--changeset mati:052-drop-old-category-column
ALTER TABLE movements DROP COLUMN category_id;
