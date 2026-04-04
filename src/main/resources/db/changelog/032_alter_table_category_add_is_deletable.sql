-- liquibase formatted sql
-- changeset mferrero:032_alter_table_category_add_is_deletable
ALTER TABLE category ADD COLUMN is_deletable BOOLEAN NOT NULL DEFAULT TRUE;
UPDATE category SET is_deletable = FALSE WHERE description = 'SERVICIOS';
