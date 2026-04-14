package api.m2.movements.services.movements;

import api.m2.movements.annotations.RequiresMembership;
import api.m2.movements.enums.MembershipDomain;
import api.m2.movements.mappers.MovementMapper;
import api.m2.movements.records.movements.MovementDeletedEvent;
import api.m2.movements.records.movements.MovementToAdd;
import api.m2.movements.records.movements.ExpenseToUpdate;
import api.m2.movements.records.movements.MovementRecord;
import api.m2.movements.repositories.MovementRepository;
import api.m2.movements.services.publishing.websockets.MovementPublishServiceWebSocket;
import api.m2.movements.exceptions.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
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
        var movementRecord = movementMapper.toRecord(movementRepository.save(movement));

        movementPublishService.publishMovementAdded(movementRecord);

        log.info("Movimiento guardado: id={}, type={}", movementRecord.id(), dto.type());
        return movementRecord;
    }

    @Transactional
    @RequiresMembership(domain = MembershipDomain.MOVEMENT, idParamIndex = 1)
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
        saved.forEach(movement ->
                movementPublishService.publishMovementAdded(
                        movementMapper.toRecord(movement)
                )
        );

        log.info("Movimientos guardados en batch: total={}", saved.size());
    }

    @Transactional
    @RequiresMembership(domain = MembershipDomain.MOVEMENT)
    public void deleteMovement(Long id) {
        var movement = movementRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Movimiento con Id" + id + " no existe"));

        Long workspaceId = movement.getWorkspace().getId();
        movementRepository.deleteById(id);
        movementPublishService.publishDeleteOfMovement(new MovementDeletedEvent(id, workspaceId));

        log.info("Movimiento eliminado correctamente: id={}", id);
    }
}