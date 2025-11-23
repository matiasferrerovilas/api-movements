package api.expenses.expenses.aspect;

import api.expenses.expenses.aspect.interfaces.PublishMovement;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@Aspect
@Slf4j
@Order(1)
public class PublishExpenseAspect {
    private final SimpMessagingTemplate messagingTemplate;

    public PublishExpenseAspect(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @AfterReturning(pointcut = "@annotation(publishMovement)", returning = "result")
    public void afterPublishEvent(JoinPoint joinPoint, Object result, PublishMovement publishMovement) {
        log.info("Publicando objeto STOMP en {}", publishMovement.routingKey());
        var eventType = publishMovement.eventType();
        var event = EventWrapper.builder()
                .eventType(eventType)
                .message(result)
                .build();
        messagingTemplate.convertAndSend(publishMovement.routingKey(), event);
    }
}
