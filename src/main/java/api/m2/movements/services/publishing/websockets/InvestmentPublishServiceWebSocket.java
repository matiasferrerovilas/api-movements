package api.m2.movements.services.publishing.websockets;

import api.m2.movements.constants.WebSocketTopics;
import api.m2.movements.enums.EventType;
import api.m2.movements.records.investments.InvestmentRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@Order(1)
public class InvestmentPublishServiceWebSocket extends WebSocketMessageService {

    public InvestmentPublishServiceWebSocket(SimpMessagingTemplate messagingTemplate) {
        super(messagingTemplate);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishInvestmentAdded(InvestmentRecord record) {
        this.publish(record, WebSocketTopics.investmentsNew(record.workspaceId()), EventType.INVESTMENT_ADDED);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishInvestmentUpdated(InvestmentRecord record) {
        this.publish(record, WebSocketTopics.investmentsUpdate(record.workspaceId()), EventType.INVESTMENT_UPDATED);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishInvestmentDeleted(InvestmentRecord record) {
        this.publish(record, WebSocketTopics.investmentsDelete(record.workspaceId()), EventType.INVESTMENT_DELETED);
    }
}
