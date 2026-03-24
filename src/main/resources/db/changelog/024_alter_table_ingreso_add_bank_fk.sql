-- liquibase formatted sql
-- changeset mati:20260324-2
ALTER TABLE ingreso
    ADD COLUMN bank_id BIGINT NULL,
    ADD CONSTRAINT fk_ingreso_bank FOREIGN KEY (bank_id) REFERENCES banks(id);

UPDATE movements m
    JOIN banks b
    ON UPPER(m.bank) = UPPER(b.description)
SET m.bank_id = b.id;

UPDATE ingreso m
    JOIN banks b
    ON UPPER(m.bank) = UPPER(b.description)
SET m.bank_id = b.id;