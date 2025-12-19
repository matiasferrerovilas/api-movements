package api.expenses.expenses.listeners;

import api.expenses.expenses.services.movements.files.MovementImportFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportFileListener {
    private final MovementImportFileService movementImportFileService;

   /* @RabbitListener(queues = RabbitConfig.QUEUE_MOVEMENT_FILE_IMPORTED)
    public void listen(EventWrapper<List<CreditCardStatement>> message) {
        log.info("Received message: {}", message);
        movementImportFileService.processList(message.getMessage());
    }*/
}
