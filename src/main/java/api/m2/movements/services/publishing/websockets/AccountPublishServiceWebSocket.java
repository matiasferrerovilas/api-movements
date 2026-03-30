package api.m2.movements.services.publishing.websockets;

import api.m2.movements.enums.EventType;
import api.m2.movements.records.invite.InvitationToGroupRecord;
import api.m2.movements.records.accounts.GroupRecord;
import api.m2.movements.records.groups.MembershipDefaultUpdatedEvent;
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
    public void publishAccountDefaultUpdated(MembershipDefaultUpdatedEvent membershipDefaultUpdatedEvent) {
        this.publish(membershipDefaultUpdatedEvent.groupUpdated(), "/topic/account/default/"
                + membershipDefaultUpdatedEvent.logInuser(), EventType.MEMBERSHIP_UPDATED);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishAccountLeft(GroupRecord groupRecord) {
        this.publish(groupRecord, "/topic/account/" + groupRecord.id() + "/leave", EventType.ACCOUNT_LEFT);
    }
}
