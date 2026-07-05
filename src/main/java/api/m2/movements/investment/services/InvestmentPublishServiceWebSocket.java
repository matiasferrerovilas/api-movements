package api.m2.movements.investment.services;

import api.m2.movements.constants.WebSocketTopics;
import api.m2.movements.movements.enums.EventType;
import api.m2.movements.investment.records.InvestmentAddedEvent;
import api.m2.movements.investment.records.InvestmentDeletedEvent;
import api.m2.movements.investment.records.InvestmentUpdatedEvent;
import api.m2.movements.movements.services.publishing.websockets.WebSocketMessageService;
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
    public void publishInvestmentAdded(InvestmentAddedEvent event) {
        var record = event.investment();
        this.publish(record, WebSocketTopics.investmentsNew(record.workspaceId()), EventType.INVESTMENT_ADDED);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishInvestmentUpdated(InvestmentUpdatedEvent event) {
        var record = event.investment();
        this.publish(record, WebSocketTopics.investmentsUpdate(record.workspaceId()), EventType.INVESTMENT_UPDATED);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishInvestmentDeleted(InvestmentDeletedEvent event) {
        var record = event.investment();
        this.publish(record, WebSocketTopics.investmentsDelete(record.workspaceId()), EventType.INVESTMENT_DELETED);
    }
}
