--liquibase formatted sql

--changeset mati:055-drop-table-investment
DROP TABLE IF EXISTS investment;

--changeset mati:055-drop-table-investment-type
DROP TABLE IF EXISTS investment_type;
