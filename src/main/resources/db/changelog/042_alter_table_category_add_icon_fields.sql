--liquibase formatted sql

--changeset mgferrero:042-alter-table-workspace-categories-add-icon-fields
ALTER TABLE workspace_categories
    ADD COLUMN icon_name VARCHAR(50) NULL COMMENT 'Nombre del ícono de Ant Design (ej: HomeOutlined)',
    ADD COLUMN icon_color VARCHAR(7) NULL COMMENT 'Color hexadecimal del ícono (ej: #faad14)';
--rollback ALTER TABLE workspace_categories DROP COLUMN icon_name, DROP COLUMN icon_color;
