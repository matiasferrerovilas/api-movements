package api.m2.movements.services.publishing.websockets;

import api.m2.movements.constants.WebSocketTopics;
import api.m2.movements.enums.EventType;
import api.m2.movements.records.invite.InvitationToWorkspaceRecord;
import api.m2.movements.records.workspaces.WorkspaceDetail;
import api.m2.movements.records.workspaces.WorkspaceRecord;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class WorkspacePublishServiceWebSocket extends WebSocketMessageService {

    public WorkspacePublishServiceWebSocket(SimpMessagingTemplate messagingTemplate) {
        super(messagingTemplate);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishInvitationAdded(InvitationToWorkspaceRecord record) {
        this.publish(record, WebSocketTopics.invitationsNew(record.invitedUserId()), EventType.INVITATION_ADDED);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishInvitationUpdated(InvitationToWorkspaceRecord record) {
        this.publish(record, WebSocketTopics.invitationsUpdate(record.invitedUserId()),
                EventType.INVITATION_CONFIRMED_REJECTED);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishWorkspaceCreated(WorkspaceRecord workspaceRecord) {
        this.publish(workspaceRecord, WebSocketTopics.workspacesNew(workspaceRecord.owner().id()),
                EventType.WORKSPACE_CREATED);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishWorkspaceLeft(WorkspaceRecord workspaceRecord) {
        this.publish(workspaceRecord, WebSocketTopics.workspacesLeave(workspaceRecord.id()),
                EventType.WORKSPACE_LEFT);
    }

    public void publishWorkspaceMembershipUpdated(WorkspaceDetail workspaceDetail, String keycloakSubject) {
        this.publish(workspaceDetail, WebSocketTopics.workspacesDefault(keycloakSubject),
                EventType.MEMBERSHIP_UPDATED);
    }

    public void publishMemberAdded(WorkspaceDetail workspaceDetail, Long workspaceId) {
        this.publish(workspaceDetail, WebSocketTopics.workspacesMembersUpdate(workspaceId),
                EventType.MEMBERSHIP_UPDATED);
    }
}
