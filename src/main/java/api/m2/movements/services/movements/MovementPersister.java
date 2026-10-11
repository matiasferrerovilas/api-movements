package api.m2.movements.services.movements;

import api.m2.movements.entities.movements.Movement;
import api.m2.movements.records.movements.MovementItemDto;
import api.m2.movements.records.movements.MovementRecord;
import api.m2.movements.records.movements.MovementToAdd;
import api.m2.movements.repositories.MovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.BiFunction;

/**
 * Pasos que comparten todas las altas de movimiento (guardar, reemplazar el desglose, armar el
 * record y publicarlo). Lo único que cambia entre {@link MovementAddService} y
 * {@link SystemMovementAddService} es cómo se enriquece el record, que cada una pasa como
 * {@code enricher}. Corre dentro de la transacción del servicio que lo llama.
 */
@Component
@RequiredArgsConstructor
public class MovementPersister {

    private final MovementRepository movementRepository;
    private final MovementItemService movementItemService;
    private final ApplicationEventPublisher eventPublisher;

    public MovementRecord persistAndPublish(Movement movement, MovementToAdd dto,
                                            BiFunction<Movement, List<MovementItemDto>, MovementRecord> enricher) {
        var saved = movementRepository.save(movement);
        movementItemService.replaceItems(saved.getId(), dto.items());
        var movementRecord = enricher.apply(saved, movementItemService.getItems(saved.getId()));

        eventPublisher.publishEvent(movementRecord);
        return movementRecord;
    }
}
