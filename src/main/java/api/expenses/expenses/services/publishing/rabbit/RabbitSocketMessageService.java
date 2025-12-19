package api.expenses.expenses.services.publishing.rabbit;

import api.expenses.expenses.aspect.EventWrapper;
import api.expenses.expenses.configuration.RabbitConfig;
import api.expenses.expenses.enums.EventType;
import api.expenses.expenses.services.publishing.MessageInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;


@Slf4j
@RequiredArgsConstructor
public abstract class RabbitSocketMessageService implements MessageInterface {
    private final RabbitTemplate rabbitTemplate;

    public void publish(Object result, String routingKey, EventType eventType) {
        log.info("Publicando objeto en Rabbit en {}", routingKey);
        var event = EventWrapper.builder()
                .eventType(eventType)
                .message(result)
                .build();
        rabbitTemplate.convertAndSend(RabbitConfig.AMQ_TOPIC_EXCHANGE, routingKey, event);

    }
}
