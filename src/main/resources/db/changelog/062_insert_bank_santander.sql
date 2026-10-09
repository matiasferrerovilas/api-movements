-- liquibase formatted sql
-- changeset mati:20261009-1
-- Santander España (EUR). Distinto de 'SANTANDER RIO' (Argentina): el import de extractos busca
-- el banco por descripción exacta.
INSERT INTO banks (description)
SELECT 'SANTANDER' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM banks WHERE description = 'SANTANDER');
