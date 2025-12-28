-- liquibase formatted sql

-- changeset mati:1766349485363-1
ALTER TABLE group_invitation
    DROP FOREIGN KEY FK_GROUPINVITATION_ON_GROUP;

ALTER TABLE group_invitation
    CHANGE COLUMN group_id account_id BIGINT NOT NULL;

ALTER TABLE group_invitation
    ADD CONSTRAINT FK_GROUPINVITATION_ON_ACCOUNT
        FOREIGN KEY (account_id)
            REFERENCES accounts(id);

CREATE INDEX idx_group_invitation_account
    ON group_invitation (account_id);
