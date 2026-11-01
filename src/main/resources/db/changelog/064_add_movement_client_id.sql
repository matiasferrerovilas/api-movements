--liquibase formatted sql

--changeset mati:064-add-movement-client-id
-- Clave de idempotencia enviada por el cliente al crear un movimiento: evita duplicados cuando
-- la app móvil reenvía su cola offline y la respuesta original se había perdido. NULL permite
-- varios movimientos sin clave (UNIQUE no cuenta los NULL).
ALTER TABLE movements ADD COLUMN client_id VARCHAR(36) NULL;
ALTER TABLE movements ADD CONSTRAINT uc_movements_workspace_client_id UNIQUE (workspace_id, client_id);
