--liquibase formatted sql

--changeset mati:046-alter-investment-type-add-category-1
ALTER TABLE investment_type ADD category VARCHAR(20) NOT NULL DEFAULT 'FCI';
