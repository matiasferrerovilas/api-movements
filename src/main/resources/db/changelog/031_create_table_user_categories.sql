-- liquibase formatted sql
-- changeset mferrero:031_create_table_user_categories
CREATE TABLE user_categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT   NOT NULL,
    category_id BIGINT   NOT NULL,
    is_active   BOOLEAN  NOT NULL DEFAULT TRUE,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_cat_user     FOREIGN KEY (user_id)     REFERENCES users(id),
    CONSTRAINT fk_user_cat_category FOREIGN KEY (category_id) REFERENCES category(id),
    CONSTRAINT uq_user_categories   UNIQUE (user_id, category_id)
);
