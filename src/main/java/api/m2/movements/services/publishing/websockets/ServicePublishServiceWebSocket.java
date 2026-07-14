package api.m2.movements.services.publishing.websockets;

import api.m2.movements.constants.WebSocketTopics;
import api.m2.movements.enums.EventType;
import api.m2.movements.records.services.ServiceAddedEvent;
import api.m2.movements.records.services.ServiceDeletedEvent;
import api.m2.movements.records.services.ServicePaidEvent;
import api.m2.movements.records.services.ServiceUpdatedEvent;
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
    public void publishServicePaid(ServicePaidEvent event) {
        var dto = event.subscription();
        this.publish(dto, WebSocketTopics.servicesUpdate(dto.workspaceId()), EventType.SERVICE_PAID);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishUpdateService(ServiceUpdatedEvent event) {
        var dto = event.subscription();
        this.publish(dto, WebSocketTopics.servicesUpdate(dto.workspaceId()), EventType.SERVICE_UPDATED);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishNewService(ServiceAddedEvent event) {
        var dto = event.subscription();
        this.publish(dto, WebSocketTopics.servicesNew(dto.workspaceId()), EventType.SERVICE_PAID);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishDeleteService(ServiceDeletedEvent event) {
        var dto = event.subscription();
        this.publish(dto, WebSocketTopics.servicesRemove(dto.workspaceId()), EventType.SERVICE_DELETED);
    }
}
