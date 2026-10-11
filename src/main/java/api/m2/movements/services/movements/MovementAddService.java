package api.m2.movements.services.movements;

import api.m2.movements.entities.movements.Movement;
import api.m2.movements.records.movements.ImportResultRecord;
import api.m2.movements.records.movements.MovementItemDto;
import api.m2.movements.records.movements.MovementRecord;
import api.m2.movements.records.movements.MovementToAdd;
import api.m2.movements.repositories.MovementRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Alta de movimientos dentro de un request autenticado. Los jobs programados (sin JWT) usan
 * {@link SystemMovementAddService}; editar y borrar viven en {@link MovementUpdateService} y
 * {@link MovementDeleteService}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MovementAddService implements MovementSaver {

    private final MovementRepository movementRepository;
    private final MovementFactory movementFactory;
    private final ApplicationEventPublisher eventPublisher;
    private final MovementDuplicateFilter movementDuplicateFilter;
    private final MovementPersister movementPersister;
    private final MovementRecordAssembler assembler;

    /** Workspace y owner salen del contexto del request; además valida rol de escritura. */
    @Transactional
    public MovementRecord saveMovement(@Valid MovementToAdd dto) {
        return this.saveAndLog(movementFactory.create(dto), dto);
    }

    @Override
    @Transactional
    public MovementRecord saveMovement(MovementToAdd dto, Long workspaceId, Long ownerId) {
        return this.saveAndLog(movementFactory.create(dto, workspaceId, ownerId), dto);
    }

    @Transactional
    public ImportResultRecord saveExpenseAll(List<@Valid MovementToAdd> list) {
        if (list == null || list.isEmpty()) {
            log.warn("Intento de guardar lista vacía de movimientos");
            return new ImportResultRecord(0, 0);
        }

        var entities = list.stream()
                .map(movementFactory::create)
                .toList();

        var workspaceId = entities.getFirst().getWorkspaceId();
        var newEntities = movementDuplicateFilter.filterNew(entities, workspaceId);
        var duplicated = entities.size() - newEntities.size();
        if (newEntities.isEmpty()) {
            log.info("Import sin movimientos nuevos: duplicados={}", duplicated);
            return new ImportResultRecord(0, duplicated);
        }

        var saved = movementRepository.saveAll(newEntities);
        var context = assembler.contextFor(workspaceId, saved);

        saved.forEach(movement ->
                eventPublisher.publishEvent(assembler.assemble(movement, context, List.of())));

        log.info("Movimientos guardados en batch: total={}, duplicados={}", saved.size(), duplicated);
        return new ImportResultRecord(saved.size(), duplicated);
    }

    private MovementRecord saveAndLog(Movement movement, MovementToAdd dto) {
        var movementRecord = movementPersister.persistAndPublish(movement, dto, this::enrich);

        log.info("Movimiento guardado: id={}, type={}", movementRecord.id(), dto.type());
        return movementRecord;
    }

    private MovementRecord enrich(Movement movement, List<MovementItemDto> items) {
        var context = assembler.contextFor(movement.getWorkspaceId(), List.of(movement));
        return assembler.assemble(movement, context, items);
    }
}
