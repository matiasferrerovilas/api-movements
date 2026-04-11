-- liquibase formatted sql

-- changeset mgferrero:036-rename-accounts-to-workspaces
RENAME TABLE accounts TO workspaces;

-- changeset mgferrero:036-rename-account-members-to-workspace-members
RENAME TABLE account_members TO workspace_members;

-- changeset mgferrero:036-rename-account-invitation-to-workspace-invitation
RENAME TABLE account_invitation TO workspace_invitation;

-- changeset mgferrero:036-rename-fk-column-workspace-members
ALTER TABLE workspace_members
    RENAME COLUMN account_id TO workspace_id;

-- changeset mgferrero:036-rename-fk-column-workspace-invitation
ALTER TABLE workspace_invitation
    RENAME COLUMN account_id TO workspace_id;

-- changeset mgferrero:036-rename-fk-column-movements
ALTER TABLE movements
    RENAME COLUMN account_id TO workspace_id;

-- changeset mgferrero:036-rename-fk-column-ingreso
ALTER TABLE ingreso
    RENAME COLUMN account_id TO workspace_id;

-- changeset mgferrero:036-rename-fk-column-subscription
ALTER TABLE subscription
    RENAME COLUMN account_id TO workspace_id;

-- changeset mgferrero:036-rename-fk-column-budget
ALTER TABLE budget
    RENAME COLUMN account_id TO workspace_id;

-- changeset mgferrero:036-update-user-settings-default-account-key
UPDATE user_settings
SET setting_key = 'DEFAULT_WORKSPACE'
WHERE setting_key = 'DEFAULT_ACCOUNT';

-- changeset mgferrero:036-enable-all-currencies
UPDATE currency
SET enabled = TRUE;
