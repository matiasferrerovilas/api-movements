package api.m2.movements.services.publishing.websockets;

import api.m2.movements.enums.EventType;
import api.m2.movements.records.accounts.AccountInvitationRecord;
import api.m2.movements.records.accounts.AccountRecord;
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
    public void publishInvitationAdded(AccountInvitationRecord accountInvitationRecord) {
        this.publish(accountInvitationRecord, "/topic/invitation/new", EventType.INVITATION_ADDED);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishInvitationUpdated(AccountInvitationRecord accountInvitationRecord) {
        this.publish(accountInvitationRecord, "/topic/invitation/update", EventType.INVITATION_CONFIRMED_REJECTED);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishAccountCreated(AccountRecord accountRecord) {
        this.publish(accountRecord, "/topic/account/new", EventType.ACCOUNT_CREATED);
    }
}
