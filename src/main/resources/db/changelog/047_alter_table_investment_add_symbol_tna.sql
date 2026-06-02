--liquibase formatted sql

--changeset mati:047-alter-investment-add-symbol-1
ALTER TABLE investment ADD symbol VARCHAR(20) NULL;

--changeset mati:047-alter-investment-add-tna-2
ALTER TABLE investment ADD tna DECIMAL(10, 4) NULL;
