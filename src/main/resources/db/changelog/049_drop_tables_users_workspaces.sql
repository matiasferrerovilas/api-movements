--liquibase formatted sql

--changeset mati:049-drop-table-workspace-invitation
DROP TABLE workspace_invitation;

--changeset mati:049-drop-table-workspace-members
DROP TABLE workspace_members;

--changeset mati:049-drop-table-workspaces
DROP TABLE workspaces;

--changeset mati:049-drop-table-users
DROP TABLE users;
