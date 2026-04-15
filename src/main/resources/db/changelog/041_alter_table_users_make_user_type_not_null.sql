--liquibase formatted sql
--changeset mferrero:041

-- Paso 1: Migrar valores existentes CONSUMER → PERSONAL, COMPANY → ENTERPRISE
UPDATE users
SET user_type = 'PERSONAL'
WHERE user_type = 'CONSUMER';

UPDATE users
SET user_type = 'ENTERPRISE'
WHERE user_type = 'COMPANY';

-- Paso 2: Asignar PERSONAL como valor por defecto a registros NULL (si existen)
UPDATE users
SET user_type = 'PERSONAL'
WHERE user_type IS NULL;

-- Paso 3: Agregar constraint NOT NULL al campo user_type
ALTER TABLE users
    MODIFY user_type VARCHAR(50) NOT NULL;
