package api.m2.movements.services.movements;

import api.m2.movements.mappers.MovementMapper;
import api.m2.movements.records.movements.MovementToAdd;
import api.m2.movements.records.movements.ExpenseToUpdate;
import api.m2.movements.records.movements.MovementRecord;
import api.m2.movements.repositories.MovementRepository;
import api.m2.movements.services.publishing.websockets.MovementPublishServiceWebSocket;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MovementAddService {

    private final MovementRepository movementRepository;
    private final MovementMapper movementMapper;
    private final MovementFactory movementFactory;
    private final MovementPublishServiceWebSocket movementPublishService;

    @Transactional
    public MovementRecord saveMovement(@Valid MovementToAdd dto) {
        var movement = movementFactory.create(dto);
        var record = movementMapper.toRecord(movementRepository.save(movement));

        movementPublishService.publishMovementAdded(record);

        log.info("Movimiento guardado: id={}, type={}", record.id(), dto.type());
        return record;
    }

    @Transactional
    public void updateMovement(@Valid ExpenseToUpdate dto, Long id) {
        var movement = movementRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found with id: " + id));

        movementMapper.updateMovement(dto, movement);
        movementFactory.applyUpdates(dto, movement);
        movementRepository.save(movement);

        log.info("Movimiento actualizado: id={}", id);
    }

    @Transactional
    public void saveExpenseAll(List<@Valid MovementToAdd> list) {
        if (list == null || list.isEmpty()) {
            log.warn("Intento de guardar lista vacía de movimientos");
            return;
        }

        var entities = list.stream()
                .map(movementFactory::create)
                .toList();

        var saved = movementRepository.saveAll(entities);
        saved
                .forEach(movement -> {
                    movementPublishService.publishMovementAdded(movementMapper.toRecord(movement));
                });

        log.info("Movimientos guardados en batch: total={}", saved.size());
    }

    @Transactional
    public void deleteMovement(Long id) {
        if (!movementRepository.existsById(id)) {
            throw new EntityNotFoundException("Movimiento con Id" + id + " no existe");
        }

        movementRepository.deleteById(id);
        movementPublishService.publishDeleteOfMovement(id);

        log.info("Movimiento eliminado correctamente: id={}", id);
    }
}