--liquibase formatted sql
--changeset mgferrero:043

-- Paso 1: Asignar valores por defecto a registros con iconName NULL
UPDATE workspace_categories
SET icon_name = 'QuestionOutlined'
WHERE icon_name IS NULL;

-- Paso 2: Asignar valores por defecto a registros con iconColor NULL
UPDATE workspace_categories
SET icon_color = '#d9d9d9'
WHERE icon_color IS NULL;

-- Paso 3: Modificar columnas a NOT NULL con DEFAULT
ALTER TABLE workspace_categories
    MODIFY icon_name VARCHAR(50) NOT NULL DEFAULT 'QuestionOutlined' COMMENT 'Nombre del ícono de Ant Design',
    MODIFY icon_color VARCHAR(7) NOT NULL DEFAULT '#d9d9d9' COMMENT 'Color hexadecimal del ícono';
--rollback ALTER TABLE workspace_categories MODIFY icon_name VARCHAR(50) NULL, MODIFY icon_color VARCHAR(7) NULL;
