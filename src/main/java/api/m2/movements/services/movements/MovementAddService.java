package api.m2.movements.services.movements;

import api.m2.movements.annotations.RequiresMembership;
import api.m2.movements.entities.commons.Category;
import api.m2.movements.entities.movements.Movement;
import api.m2.movements.enums.DefaultCategory;
import api.m2.movements.records.categories.CategoryCorrectedEvent;
import api.m2.movements.enums.MembershipDomain;
import api.m2.movements.mappers.MovementMapper;
import api.m2.movements.records.movements.ImportResultRecord;
import api.m2.movements.records.movements.MovementDeletedEvent;
import api.m2.movements.records.movements.MovementToAdd;
import api.m2.movements.records.movements.ExpenseToUpdate;
import api.m2.movements.records.movements.MovementRecord;
import api.m2.movements.records.movements.MovementItemDto;
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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Alta, edición y baja de movimientos dentro de un request autenticado. Los jobs programados
 * (sin JWT) usan {@link SystemMovementAddService}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MovementAddService implements MovementSaver {

    private final MovementRepository movementRepository;
    private final MovementMapper movementMapper;
    private final MovementFactory movementFactory;
    private final MovementItemService movementItemService;
    private final ApplicationEventPublisher eventPublisher;
    private final WorkspaceQueryService workspaceQueryService;
    private final UserService userService;
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

    private MovementRecord enrich(Movement movement, List<MovementItemDto> items) {
        var workspace = new WorkspaceBaseRecord(movement.getWorkspaceId(),
                workspaceQueryService.findWorkspaceNameById(movement.getWorkspaceId()));
        var ownerNamesById = userService.getUserNamesByIds(List.of(movement.getOwnerId()));
        return assembler.assemble(movement, workspace, ownerNamesById, items,
                assembler.iconsOf(movement.getWorkspaceId()));
    }

    private MovementRecord saveAndLog(Movement movement, MovementToAdd dto) {
        var movementRecord = movementPersister.persistAndPublish(movement, dto, this::enrich);

        log.info("Movimiento guardado: id={}, type={}", movementRecord.id(), dto.type());
        return movementRecord;
    }

    @Transactional
    @RequiresMembership(domain = MembershipDomain.MOVEMENT, idParamIndex = 1)
    public void updateMovement(@Valid ExpenseToUpdate dto, Long id) {
        var movement = movementRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found with id: " + id));

        var previousCategoryIds = this.categoryIds(movement);
        movementMapper.updateMovement(dto, movement);
        movementFactory.applyUpdates(dto, movement);
        movementRepository.save(movement);
        this.publishCategoryCorrection(dto, movement, previousCategoryIds);
        if (dto.items() != null) {
            movementItemService.replaceItems(id, dto.items());
        }

        log.info("Movimiento actualizado: id={}", id);
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

        var workspace = new WorkspaceBaseRecord(workspaceId, workspaceQueryService.findWorkspaceNameById(workspaceId));
        var ownerIds = saved.stream().map(Movement::getOwnerId).distinct().toList();
        var ownerNamesById = userService.getUserNamesByIds(ownerIds);
        var iconMap = assembler.iconsOf(workspaceId);

        saved.forEach(movement ->
                eventPublisher.publishEvent(assembler.assemble(movement, workspace, ownerNamesById, List.of(), iconMap)));

        log.info("Movimientos guardados en batch: total={}, duplicados={}", saved.size(), duplicated);
        return new ImportResultRecord(saved.size(), duplicated);
    }

    @Transactional
    @RequiresMembership(domain = MembershipDomain.MOVEMENT)
    public void deleteMovement(Long id) {
        var movement = movementRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Movimiento con Id" + id + " no existe"));

        Long workspaceId = movement.getWorkspaceId();
        movementItemService.replaceItems(id, null);
        movementRepository.deleteById(id);
        eventPublisher.publishEvent(new MovementDeletedEvent(id, workspaceId));

        log.info("Movimiento eliminado correctamente: id={}", id);
    }

    /**
     * Borra todos los movimientos cuyo owner es el usuario autenticado, en cualquier workspace.
     * Los movimientos que otros miembros cargaron en un workspace compartido no se tocan.
     */
    @Transactional
    public int deleteAllMovementsOfCurrentUser() {
        var ownerId = userService.getMe().id();
        var movements = movementRepository.findAllByOwnerId(ownerId);
        if (movements.isEmpty()) {
            return 0;
        }

        movementItemService.deleteItemsOf(movements.stream().map(Movement::getId).toList());
        movementRepository.deleteAll(movements);
        movements.forEach(movement ->
                eventPublisher.publishEvent(new MovementDeletedEvent(movement.getId(), movement.getWorkspaceId())));

        log.info("Movimientos eliminados del usuario: ownerId={}, total={}", ownerId, movements.size());
        return movements.size();
    }

    // El front manda las categorías en cada edición aunque no cambien: solo es corrección si el
    // conjunto cambió y quedó una única categoría real (con varias no hay a cuál asociar el comercio).
    private void publishCategoryCorrection(ExpenseToUpdate dto, Movement movement, Set<Long> previousCategoryIds) {
        if (dto.categories() == null || previousCategoryIds.equals(this.categoryIds(movement))) {
            return;
        }
        var real = movement.getCategories().stream()
                .filter(category -> !DefaultCategory.SIN_CATEGORIA.getDescription().equals(category.getDescription()))
                .toList();
        if (real.size() == 1) {
            eventPublisher.publishEvent(new CategoryCorrectedEvent(
                    movement.getWorkspaceId(), movement.getDescription(), real.getFirst().getId()));
        }
    }

    private Set<Long> categoryIds(Movement movement) {
        return movement.getCategories().stream().map(Category::getId).collect(Collectors.toSet());
    }
}
