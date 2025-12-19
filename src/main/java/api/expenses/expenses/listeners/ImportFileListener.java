package api.expenses.expenses.listeners;

import api.expenses.expenses.aspect.EventWrapper;
import api.expenses.expenses.configuration.RabbitConfig;
import api.expenses.expenses.records.movements.file.CreditCardStatement;
import api.expenses.expenses.services.movements.files.MovementImportFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportFileListener {
    private final MovementImportFileService movementImportFileService;

    @RabbitListener(queues = RabbitConfig.QUEUE_MOVEMENT_FILE_IMPORTED)
    public void listen(EventWrapper<List<CreditCardStatement>> message) {
        log.info("Received message: {}", message);
        movementImportFileService.processList(message.getMessage());
    }
}
