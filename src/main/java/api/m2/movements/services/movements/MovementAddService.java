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
import api.m2.movements.clients.identity.response.UserBaseRecord;
import api.m2.movements.records.categories.CategoryUpdateRecord;
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

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MovementAddService {

    private final MovementRepository movementRepository;
    private final MovementMapper movementMapper;
    private final MovementFactory movementFactory;
    private final MovementItemService movementItemService;
    private final ApplicationEventPublisher eventPublisher;
    private final WorkspaceQueryService workspaceQueryService;
    private final UserService userService;
    private final MovementDuplicateFilter movementDuplicateFilter;

    @Transactional
    public MovementRecord saveMovement(@Valid MovementToAdd dto) {
        var movement = movementFactory.create(dto);
        var saved = movementRepository.save(movement);
        movementItemService.replaceItems(saved.getId(), dto.items());
        var movementRecord = this.enrich(saved, movementItemService.getItems(saved.getId()));

        eventPublisher.publishEvent(movementRecord);

        log.info("Movimiento guardado: id={}, type={}", movementRecord.id(), dto.type());
        return movementRecord;
    }

    @Transactional
    public MovementRecord saveMovement(@Valid MovementToAdd dto, Long workspaceId, Long ownerId) {
        var movement = movementFactory.create(dto, workspaceId, ownerId);
        var saved = movementRepository.save(movement);
        movementItemService.replaceItems(saved.getId(), dto.items());
        var movementRecord = this.enrich(saved, movementItemService.getItems(saved.getId()));

        eventPublisher.publishEvent(movementRecord);

        log.info("Movimiento guardado: id={}, type={}", movementRecord.id(), dto.type());
        return movementRecord;
    }

    /**
     * Variante para flujos internos (jobs programados) que no corren dentro de un request
     * autenticado y por lo tanto no tienen un JWT de usuario disponible para llamar a
     * api-identity. Omite el enriquecimiento con nombre de workspace/owner en vez de fallar
     * con {@link api.m2.movements.exceptions.PermissionDeniedException}.
     */
    @Transactional
    public MovementRecord saveSystemMovement(@Valid MovementToAdd dto, Long workspaceId, Long ownerId) {
        var movement = movementFactory.create(dto, workspaceId, ownerId);
        var saved = movementRepository.save(movement);
        movementItemService.replaceItems(saved.getId(), dto.items());
        var movementRecord = this.enrichWithoutIdentity(saved, movementItemService.getItems(saved.getId()));

        eventPublisher.publishEvent(movementRecord);

        log.info("Movimiento de sistema guardado: id={}, type={}", movementRecord.id(), dto.type());
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

        saved.forEach(movement ->
                eventPublisher.publishEvent(this.buildRecord(movement, workspace, ownerNamesById, List.of())));

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

    private MovementRecord enrich(Movement movement, List<MovementItemDto> items) {
        var workspace = new WorkspaceBaseRecord(movement.getWorkspaceId(),
                workspaceQueryService.findWorkspaceNameById(movement.getWorkspaceId()));
        var ownerNamesById = userService.getUserNamesByIds(List.of(movement.getOwnerId()));
        return this.buildRecord(movement, workspace, ownerNamesById, items);
    }

    private MovementRecord enrichWithoutIdentity(Movement movement, List<MovementItemDto> items) {
        var workspace = new WorkspaceBaseRecord(movement.getWorkspaceId(), null);
        return this.buildRecord(movement, workspace, Map.of(), items);
    }

    private MovementRecord buildRecord(Movement movement, WorkspaceBaseRecord workspace,
                                        Map<Long, String> ownerNamesById, List<MovementItemDto> items) {
        var baseRecord = movementMapper.toRecord(movement);
        var metadata = new MovementRecord.Metadata(
                new UserBaseRecord(ownerNamesById.get(movement.getOwnerId()), movement.getOwnerId()),
                workspace, movement.getExchangeRate(), null);
        return new MovementRecord(
                baseRecord.id(), baseRecord.amount(), baseRecord.description(), baseRecord.date(),
                baseRecord.createdAt(), baseRecord.updatedAt(), baseRecord.categories(), baseRecord.currency(),
                baseRecord.bank(), baseRecord.type(), baseRecord.cuotaActual(), baseRecord.cuotasTotales(),
                baseRecord.lastCreditPayment(), metadata, items);
    }

    /**
     * Genera la próxima cuota de cada compra en cuotas (CREDITO) que quedó pendiente el mes
     * pasado — clona el movimiento anterior tal cual (monto, categoría, moneda, banco,
     * lastCreditPayment) y solo cambia cuotaActual (+1) y date (mes actual). No necesita ningún
     * id que agrupe las cuotas de una misma compra: alcanza con mirar el mes anterior, porque
     * cada fila ya lleva su propio estado (cuotaActual) — la cuota que generamos hoy es, por
     * definición, la fila del mes anterior con cuotaActual todavía distinto de cuotasTotales.
     * Llamado por {@link CreditInstallmentJob}.
     */
    @Transactional
    public int generateNextCreditInstallments(YearMonth previousMonth) {
        var pending = movementRepository.findCreditoMovementsWithPendingInstallments(
                previousMonth.getYear(), previousMonth.getMonthValue());
        log.info("Generando {} próximas cuotas de crédito (mes anterior: {})", pending.size(), previousMonth);

        for (var previous : pending) {
            this.saveSystemMovement(this.buildNextInstallment(previous), previous.getWorkspaceId(), previous.getOwnerId());
        }

        return pending.size();
    }

    private MovementToAdd buildNextInstallment(Movement previous) {
        var categories = previous.getCategories().stream()
                .map(c -> new CategoryUpdateRecord(null, c.getDescription()))
                .toList();

        return new MovementToAdd(
                previous.getAmount(),
                LocalDate.now(ZoneOffset.UTC),
                previous.getDescription(),
                categories,
                previous.getType().name(),
                previous.getCurrency().getSymbol(),
                previous.getCuotaActual() + 1,
                previous.getCuotasTotales(),
                previous.getBank() != null ? previous.getBank().getDescription() : null,
                previous.getLastCreditPayment(),
                null);
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
