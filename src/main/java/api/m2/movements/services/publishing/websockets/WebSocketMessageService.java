package api.m2.movements.services.publishing.websockets;

import api.m2.movements.records.events.EventWrapper;
import api.m2.movements.enums.EventType;
import api.m2.movements.services.publishing.MessageInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Slf4j
@RequiredArgsConstructor
public abstract class WebSocketMessageService implements MessageInterface {
    private final SimpMessagingTemplate messagingTemplate;

    public void publish(Object result, String topic, EventType eventType) {
        log.info("Publicando objeto STOMP en {}", topic);
        var event = new EventWrapper<>(eventType, result);
        messagingTemplate.convertAndSend(topic, event);
    }
}
