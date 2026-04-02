package api.m2.movements.services.publishing.websockets;

import api.m2.movements.enums.EventType;
import api.m2.movements.records.accounts.GroupDetail;
import api.m2.movements.records.invite.InvitationToGroupRecord;
import api.m2.movements.records.accounts.GroupRecord;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class AccountPublishServiceWebSocket extends WebSocketMessageService {

    public AccountPublishServiceWebSocket(SimpMessagingTemplate messagingTemplate) {
        super(messagingTemplate);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishInvitationAdded(InvitationToGroupRecord invitationToGroupRecord) {
        this.publish(invitationToGroupRecord, "/topic/invitation/" + invitationToGroupRecord.invitedUserId() + "/new", EventType.INVITATION_ADDED);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishInvitationUpdated(InvitationToGroupRecord invitationToGroupRecord) {
        this.publish(invitationToGroupRecord,
            "/topic/invitation/" + invitationToGroupRecord.invitedUserId() + "/update",
            EventType.INVITATION_CONFIRMED_REJECTED);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishAccountCreated(GroupRecord groupRecord) {
        this.publish(groupRecord, "/topic/account/" + groupRecord.owner().id() + "/new", EventType.ACCOUNT_CREATED);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishAccountLeft(GroupRecord groupRecord) {
        this.publish(groupRecord, "/topic/account/" + groupRecord.id() + "/leave", EventType.ACCOUNT_LEFT);
    }

    public void publishGroupMembershipUpdated(GroupDetail groupDetail, String keycloakSubject) {
        this.publish(groupDetail, "/topic/account/default/" + keycloakSubject, EventType.MEMBERSHIP_UPDATED);
    }

    public void publishMemberAdded(GroupDetail groupDetail, Long accountId) {
        this.publish(groupDetail, "/topic/account/" + accountId + "/members/update", EventType.MEMBERSHIP_UPDATED);
    }
}
