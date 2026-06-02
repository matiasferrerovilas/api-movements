package api.m2.movements.services.movements;

import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.records.subscriptions.SubscriptionMovementSyncEvent;
import api.m2.movements.repositories.MovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SyncMovementsService {

    private final MovementRepository movementRepository;

    @Transactional
    public void syncSubscriptionMovement(SubscriptionMovementSyncEvent event) {
        var movement = movementRepository.findByDescriptionAndAccountAndMonth(
                        "Servicio Pagado " + event.oldDescription(),
                        event.workspaceId(),
                        event.year(),
                        event.month())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró el movimiento asociado al servicio"));

        if (event.newAmount() != null) {
            movement.setAmount(event.newAmount());
        }
        if (event.newDescription() != null) {
            movement.setDescription("Servicio Pagado " + event.newDescription());
        }
        if (event.newDate() != null) {
            movement.setDate(event.newDate());
        }

        movementRepository.save(movement);
    }
}
