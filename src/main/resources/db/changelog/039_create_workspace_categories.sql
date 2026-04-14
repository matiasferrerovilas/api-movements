-- liquibase formatted sql
-- changeset mferrero:039_create_workspace_categories

-- 1. Crear tabla workspace_categories
CREATE TABLE workspace_categories (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_id BIGINT   NOT NULL,
    category_id  BIGINT   NOT NULL,
    is_active    BOOLEAN  NOT NULL DEFAULT TRUE,
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ws_cat_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id),
    CONSTRAINT fk_ws_cat_category  FOREIGN KEY (category_id)  REFERENCES category(id),
    CONSTRAINT uq_workspace_categories UNIQUE (workspace_id, category_id)
);

-- 2. Migrar datos: copiar categorías del owner a cada workspace
INSERT INTO workspace_categories (workspace_id, category_id, is_active)
SELECT w.id, uc.category_id, uc.is_active
FROM workspaces w
JOIN user_categories uc ON uc.user_id = w.owner_id;

-- 3. Eliminar tabla user_categories (ya no se usa)
DROP TABLE user_categories;
