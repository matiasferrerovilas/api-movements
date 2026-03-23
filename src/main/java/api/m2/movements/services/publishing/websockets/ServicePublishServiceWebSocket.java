package api.m2.movements.services.publishing.websockets;

import api.m2.movements.enums.EventType;
import api.m2.movements.records.services.SubscriptionRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@Order(1)
public class ServicePublishServiceWebSocket extends WebSocketMessageService {

    public ServicePublishServiceWebSocket(SimpMessagingTemplate messagingTemplate) {
        super(messagingTemplate);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishServicePaid(SubscriptionRecord dto) {
        this.publish(dto, "/topic/servicios/update", EventType.SERVICE_PAID);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishUpdateService(SubscriptionRecord dto) {
        this.publish(dto, "/topic/servicios/update", EventType.SERVICE_UPDATED);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishNewService(SubscriptionRecord dto) {
        this.publish(dto, "/topic/servicios/new", EventType.SERVICE_PAID);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishDeleteService(SubscriptionRecord dto) {
        this.publish(dto, "/topic/servicios/remove", EventType.SERVICE_DELETED);
    }
}
