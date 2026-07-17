package api.m2.movements.services.movements;

import api.m2.movements.annotations.RequiresMembership;
import api.m2.movements.entities.movements.Movement;
import api.m2.movements.enums.MembershipDomain;
import api.m2.movements.mappers.MovementMapper;
import api.m2.movements.records.movements.MovementDeletedEvent;
import api.m2.movements.records.movements.MovementToAdd;
import api.m2.movements.records.movements.ExpenseToUpdate;
import api.m2.movements.records.movements.MovementRecord;
import api.m2.movements.records.users.UserBaseRecord;
import api.m2.movements.records.workspaces.WorkspaceBaseRecord;
import api.m2.movements.repositories.MovementRepository;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.services.user.UserService;
import api.m2.movements.services.workspaces.WorkspaceQueryService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class MovementAddService {

    private final MovementRepository movementRepository;
    private final MovementMapper movementMapper;
    private final MovementFactory movementFactory;
    private final ApplicationEventPublisher eventPublisher;
    private final WorkspaceQueryService workspaceQueryService;
    private final UserService userService;

    @Transactional
    public MovementRecord saveMovement(@Valid MovementToAdd dto) {
        var movement = movementFactory.create(dto);
        var movementRecord = this.enrich(movementRepository.save(movement));

        eventPublisher.publishEvent(movementRecord);

        log.info("Movimiento guardado: id={}, type={}", movementRecord.id(), dto.type());
        return movementRecord;
    }

    @Transactional
    public MovementRecord saveMovement(@Valid MovementToAdd dto, Long workspaceId, Long ownerId) {
        var movement = movementFactory.create(dto, workspaceId, ownerId);
        var movementRecord = this.enrich(movementRepository.save(movement));

        eventPublisher.publishEvent(movementRecord);

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

        var workspaceId = saved.getFirst().getWorkspaceId();
        var workspace = new WorkspaceBaseRecord(workspaceId, workspaceQueryService.findWorkspaceNameById(workspaceId));
        var ownerIds = saved.stream().map(Movement::getOwnerId).distinct().toList();
        var ownerNamesById = userService.getUserNamesByIds(ownerIds);

        saved.forEach(movement ->
                eventPublisher.publishEvent(this.buildRecord(movement, workspace, ownerNamesById)));

        log.info("Movimientos guardados en batch: total={}", saved.size());
    }

    @Transactional
    @RequiresMembership(domain = MembershipDomain.MOVEMENT)
    public void deleteMovement(Long id) {
        var movement = movementRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Movimiento con Id" + id + " no existe"));

        Long workspaceId = movement.getWorkspaceId();
        movementRepository.deleteById(id);
        eventPublisher.publishEvent(new MovementDeletedEvent(id, workspaceId));

        log.info("Movimiento eliminado correctamente: id={}", id);
    }

    private MovementRecord enrich(Movement movement) {
        var workspace = new WorkspaceBaseRecord(movement.getWorkspaceId(),
                workspaceQueryService.findWorkspaceNameById(movement.getWorkspaceId()));
        var ownerNamesById = userService.getUserNamesByIds(List.of(movement.getOwnerId()));
        return this.buildRecord(movement, workspace, ownerNamesById);
    }

    private MovementRecord buildRecord(Movement movement, WorkspaceBaseRecord workspace, Map<Long, String> ownerNamesById) {
        var baseRecord = movementMapper.toRecord(movement);
        var metadata = new MovementRecord.Metadata(
                new UserBaseRecord(ownerNamesById.get(movement.getOwnerId()), movement.getOwnerId()),
                workspace, movement.getExchangeRate(), null);
        return new MovementRecord(
                baseRecord.id(), baseRecord.amount(), baseRecord.description(), baseRecord.date(),
                baseRecord.createdAt(), baseRecord.updatedAt(), baseRecord.category(), baseRecord.currency(),
                baseRecord.bank(), baseRecord.type(), baseRecord.cuotaActual(), baseRecord.cuotasTotales(), metadata);
    }
}