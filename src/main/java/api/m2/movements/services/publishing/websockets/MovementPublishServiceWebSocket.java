package api.m2.movements.services.publishing.websockets;

import api.m2.movements.constants.WebSocketTopics;
import api.m2.movements.enums.EventType;
import api.m2.movements.records.movements.MovementDeletedEvent;
import api.m2.movements.records.movements.MovementRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@Slf4j
@Order(1)
public class MovementPublishServiceWebSocket extends WebSocketMessageService {

    public MovementPublishServiceWebSocket(SimpMessagingTemplate messagingTemplate) {
        super(messagingTemplate);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishMovementAdded(MovementRecord record) {
        this.publish(record, WebSocketTopics.movementsNew(record.account().id()), EventType.MOVEMENT_ADDED);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishDeleteOfMovement(MovementDeletedEvent event) {
        this.publish(event.movementId(), WebSocketTopics.movementsDelete(event.workspaceId()), EventType.MOVEMENT_DELETED);
    }
}