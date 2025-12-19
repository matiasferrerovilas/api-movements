package api.expenses.expenses.services.publishing.websockets;

import api.expenses.expenses.aspect.EventWrapper;
import api.expenses.expenses.enums.EventType;
import api.expenses.expenses.services.publishing.MessageInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Slf4j
@RequiredArgsConstructor
public abstract class WebSocketMessageService implements MessageInterface {
    private final SimpMessagingTemplate messagingTemplate;

    public void publish(Object result, String topic, EventType eventType) {
        log.info("Publicando objeto STOMP en {}", topic);
        var event = EventWrapper.builder()
                .eventType(eventType)
                .message(result)
                .build();
        messagingTemplate.convertAndSend(topic, event);
    }
}
