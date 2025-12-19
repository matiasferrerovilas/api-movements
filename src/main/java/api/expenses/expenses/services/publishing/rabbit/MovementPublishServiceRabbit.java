package api.expenses.expenses.services.publishing.rabbit;

import api.expenses.expenses.enums.EventType;
import api.expenses.expenses.records.movements.file.MovementFileRequest;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class MovementPublishServiceRabbit extends RabbitSocketMessageService {
    public MovementPublishServiceRabbit(RabbitTemplate rabbitTemplate) {
        super(rabbitTemplate);
    }

    public void publishMovementFile(MovementFileRequest record) {
        this.publish(record, "n8n.import.file", EventType.MOVEMENT_FILE_ADDED);
    }
}
