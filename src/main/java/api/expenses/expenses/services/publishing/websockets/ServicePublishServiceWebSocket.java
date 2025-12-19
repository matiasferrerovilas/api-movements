package api.expenses.expenses.services.publishing.websockets;

import api.expenses.expenses.enums.EventType;
import api.expenses.expenses.records.services.ServiceRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Order(1)
public class ServicePublishServiceWebSocket extends WebSocketMessageService {

    public ServicePublishServiceWebSocket(SimpMessagingTemplate messagingTemplate) {
        super(messagingTemplate);
    }

    public void publishServicePaid(ServiceRecord dto) {
        this.publish(dto, "/topic/servicios/update", EventType.SERVICE_PAID);
    }

    public void publishUpdateService(ServiceRecord dto) {
        this.publish(dto, "/topic/servicios/update", EventType.SERVICE_UPDATED);
    }

    public void publishDeleteService(ServiceRecord dto) {
        this.publish(dto, "/topic/servicios/remove", EventType.SERVICE_DELETED);
    }
}
