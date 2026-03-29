-- liquibase formatted sql
-- changeset mgferrero:025-create-user-settings
CREATE TABLE user_settings (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    setting_key VARCHAR(50)  NOT NULL,
    setting_value BIGINT     NOT NULL,
    created_at  DATETIME     NULL,
    updated_at  DATETIME     NULL,
    CONSTRAINT uq_user_setting UNIQUE (user_id, setting_key),
    CONSTRAINT fk_user_settings_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- changeset mgferrero:025-migrate-default-account
INSERT INTO user_settings (user_id, setting_key, setting_value, created_at, updated_at)
SELECT am.user_id, 'DEFAULT_ACCOUNT', am.account_id, NOW(), NOW()
FROM account_members am
WHERE am.is_default = TRUE;
