-- liquibase formatted sql

-- changeset mati:1765059563447-1
ALTER TABLE movements
    CHANGE COLUMN movement_type type VARCHAR(50) NOT NULL;

