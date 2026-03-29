--liquibase formatted sql

-- changeset mgferrero:026-drop-is-default-from-account-members
ALTER TABLE account_members DROP COLUMN is_default;
