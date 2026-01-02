package api.expenses.expenses.services.publishing.websockets;

import api.expenses.expenses.enums.EventType;
import api.expenses.expenses.records.movements.MovementRecord;
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
        this.publish(record, "/topic/movimientos/new", EventType.MOVEMENT_ADDED);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishDeleteOfMovement(Long id) {
        this.publish(id, "/topic/movimientos/delete", EventType.MOVEMENT_DELETED);
    }
}