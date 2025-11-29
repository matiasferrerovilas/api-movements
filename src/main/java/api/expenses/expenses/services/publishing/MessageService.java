package api.expenses.expenses.services.publishing;

import api.expenses.expenses.aspect.EventWrapper;
import api.expenses.expenses.enums.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Slf4j
@RequiredArgsConstructor
public abstract class MessageService {
    private final SimpMessagingTemplate messagingTemplate;

    protected void publish(Object result, String topic, EventType eventType) {
        log.info("Publicando objeto STOMP en {}", topic);
        var event = EventWrapper.builder()
                .eventType(eventType)
                .message(result)
                .build();
        messagingTemplate.convertAndSend(topic, event);
    }
}
