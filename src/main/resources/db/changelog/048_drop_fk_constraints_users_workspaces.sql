--liquibase formatted sql

--changeset mati:048-drop-fk-movements-user
ALTER TABLE movements DROP FOREIGN KEY FK_MOVEMENTS_ON_USER;

--changeset mati:048-drop-fk-movements-account
ALTER TABLE movements DROP FOREIGN KEY FK_MOVEMENTS_ON_ACCOUNT;

--changeset mati:048-drop-fk-ingreso-user
ALTER TABLE ingreso DROP FOREIGN KEY FK_INGRESO_ON_USER;

--changeset mati:048-drop-fk-ingreso-account
ALTER TABLE ingreso DROP FOREIGN KEY FK_INGRESO_ON_ACCOUNT;

--changeset mati:048-drop-fk-subscription-user
ALTER TABLE subscription DROP FOREIGN KEY FK_SERVICES_ON_USER;

--changeset mati:048-drop-fk-subscription-account
ALTER TABLE subscription DROP FOREIGN KEY FK_SERVICES_ON_ACCOUNT;

--changeset mati:048-drop-fk-budget-account
ALTER TABLE budget DROP FOREIGN KEY fk_budget_account;

--changeset mati:048-drop-fk-workspaces-owner
ALTER TABLE workspaces DROP FOREIGN KEY FK_ACCOUNTS_ON_OWNER;

--changeset mati:048-drop-fk-workspace-members-user
ALTER TABLE workspace_members DROP FOREIGN KEY FK_ACCOUNT_MEMBERS_ON_USER;

--changeset mati:048-drop-fk-workspace-members-account
ALTER TABLE workspace_members DROP FOREIGN KEY FK_ACCOUNT_MEMBERS_ON_ACCOUNT;

--changeset mati:048-drop-fk-workspace-invitation-user
ALTER TABLE workspace_invitation DROP FOREIGN KEY FK_GROUPINVITATION_ON_USER;

--changeset mati:048-drop-fk-workspace-invitation-invited-by
ALTER TABLE workspace_invitation DROP FOREIGN KEY FK_GROUPINVITATION_ON_INVITED_BY;

--changeset mati:048-drop-fk-workspace-invitation-account
ALTER TABLE workspace_invitation DROP FOREIGN KEY FK_GROUPINVITATION_ON_ACCOUNT;

--changeset mati:048-drop-fk-user-settings-user
ALTER TABLE user_settings DROP FOREIGN KEY fk_user_settings_user;

--changeset mati:048-drop-fk-user-banks-user
ALTER TABLE user_banks DROP FOREIGN KEY fk_user_banks_user;

--changeset mati:048-drop-fk-monthly-summary-snapshot-user
ALTER TABLE monthly_summary_snapshot DROP FOREIGN KEY fk_snapshot_user;

--changeset mati:048-drop-fk-workspace-categories-workspace
ALTER TABLE workspace_categories DROP FOREIGN KEY fk_ws_cat_workspace;

--changeset mati:048-drop-fk-investment-type-workspace
ALTER TABLE investment_type DROP FOREIGN KEY fk_investment_type_workspace;

--changeset mati:048-drop-fk-investment-workspace
ALTER TABLE investment DROP FOREIGN KEY fk_investment_workspace;

--changeset mati:048-drop-fk-investment-owner
ALTER TABLE investment DROP FOREIGN KEY fk_investment_owner;
