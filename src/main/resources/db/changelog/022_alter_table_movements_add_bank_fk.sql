-- liquibase formatted sql
-- changeset mati:20260324-2
ALTER TABLE movements
    ADD COLUMN bank_id BIGINT NULL,
    ADD CONSTRAINT fk_movements_bank FOREIGN KEY (bank_id) REFERENCES banks(id);
