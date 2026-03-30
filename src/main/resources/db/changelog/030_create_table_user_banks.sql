-- liquibase formatted sql
-- changeset mferrero:030_create_table_user_banks
CREATE TABLE user_banks (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    bank_id    BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_banks_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_banks_bank FOREIGN KEY (bank_id) REFERENCES banks(id),
    CONSTRAINT uq_user_banks UNIQUE (user_id, bank_id)
);
