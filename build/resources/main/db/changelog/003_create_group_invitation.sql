-- liquibase formatted sql

-- changeset mati:1762984071335-1
CREATE TABLE group_invitation
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    user_id    BIGINT                NOT NULL,
    group_id   BIGINT                NOT NULL,
    invited_by BIGINT                NOT NULL,
    status     VARCHAR(20)           NOT NULL,
    created_at datetime              NULL,
    updated_at datetime              NULL,
    CONSTRAINT pk_groupinvitation PRIMARY KEY (id)
);

-- changeset mati:1762984071335-2
ALTER TABLE group_invitation
    ADD CONSTRAINT FK_GROUPINVITATION_ON_GROUP FOREIGN KEY (group_id) REFERENCES user_groups (id);

-- changeset mati:1762984071335-3
ALTER TABLE group_invitation
    ADD CONSTRAINT FK_GROUPINVITATION_ON_INVITED_BY FOREIGN KEY (invited_by) REFERENCES users (id);

-- changeset mati:1762984071335-4
ALTER TABLE group_invitation
    ADD CONSTRAINT FK_GROUPINVITATION_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);


INSERT INTO user_groups (`description`) VALUES ('DEFAULT');