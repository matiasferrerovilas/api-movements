-- liquibase formatted sql

-- changeset mati:050-clear-stale-user-scoped-snapshots
DELETE FROM monthly_summary_snapshot;

-- changeset mati:050-drop-unique-constraint-snapshot-user
ALTER TABLE monthly_summary_snapshot
    DROP INDEX uq_snapshot_user_year_month;

-- changeset mati:050-rename-column-snapshot-user-to-workspace
ALTER TABLE monthly_summary_snapshot
    RENAME COLUMN user_id TO workspace_id;

-- changeset mati:050-add-unique-constraint-snapshot-workspace
ALTER TABLE monthly_summary_snapshot
    ADD CONSTRAINT uq_snapshot_workspace_year_month UNIQUE (workspace_id, year, month);
