package api.expenses.expenses.services.publishing.websockets;

import api.expenses.expenses.enums.EventType;
import api.expenses.expenses.records.accounts.AccountRecord;
import api.expenses.expenses.records.groups.GroupInvitationRecord;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountPublishServiceWebSocket extends WebSocketMessageService {


    public AccountPublishServiceWebSocket(SimpMessagingTemplate messagingTemplate) {
        super(messagingTemplate);
    }

    public void publishInvitationAdded(List<GroupInvitationRecord> recordList) {
        this.publish(recordList, "/topic/invitation/new", EventType.INVITATION_ADDED);
    }

    public void publishInvitationUpdated(List<GroupInvitationRecord> recordList) {
        this.publish(recordList, "/topic/invitation/update", EventType.INVITATION_CONFIRMED_REJECTED);
    }

    public void publishAccountCreated(AccountRecord accountRecord) {
        this.publish(accountRecord, "/topic/account/new", EventType.ACCOUNT_CREATED);
    }
}
