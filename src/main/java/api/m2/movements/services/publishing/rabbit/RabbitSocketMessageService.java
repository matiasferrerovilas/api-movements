package api.m2.movements.services.publishing.rabbit;

import api.m2.movements.aspect.EventWrapper;
import api.m2.movements.configuration.RabbitConfig;
import api.m2.movements.enums.EventType;
import api.m2.movements.services.publishing.MessageInterface;
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
