package api.m2.movements.services.movements;

import api.m2.movements.entities.Movement;
import api.m2.movements.entities.WorkspaceCategory;
import api.m2.movements.mappers.CategoryMapper;
import api.m2.movements.mappers.MovementMapper;
import api.m2.movements.records.categories.CategoryRecord;
import api.m2.movements.records.movements.MovementRecord;
import api.m2.movements.records.movements.MovementSearchFilterRecord;
import api.m2.movements.repositories.MovementRepository;
import api.m2.movements.repositories.WorkspaceCategoryRepository;
import api.m2.movements.services.workspaces.WorkspaceContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovementGetService {
    private final MovementRepository movementRepository;
    private final MovementMapper movementMapper;
    private final CategoryMapper categoryMapper;
    private final WorkspaceContextService workspaceContextService;
    private final WorkspaceCategoryRepository workspaceCategoryRepository;

    @Transactional(readOnly = true)
    public Page<@NonNull MovementRecord> getExpensesBy(MovementSearchFilterRecord filter, Pageable page) {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();

        // Query 1: Obtener movimientos paginados del workspace
        Page<Movement> movements = movementRepository.getExpenseBy(List.of(workspaceId), filter, page);

        // Query 2: Obtener todas las categorías activas del workspace con sus iconos
        List<WorkspaceCategory> categories = workspaceCategoryRepository
                .findByWorkspaceIdAndIsActiveTrue(workspaceId);

        // Crear Map para lookup O(1) por category.id
        Map<Long, WorkspaceCategory> iconMap = categories.stream()
                .collect(Collectors.toMap(wc -> wc.getCategory().getId(), wc -> wc));

        // Mapear cada movimiento enriquecido con iconos
        return movements.map(movement -> this.enrichMovementWithIcons(movement, iconMap));
    }

    /**
     * Enriquece un movimiento con iconos desde el Map de WorkspaceCategories.
     *
     * @param movement el movimiento a enriquecer
     * @param iconMap map de category.id -> WorkspaceCategory con iconos
     * @return MovementRecord con iconos
     */
    private MovementRecord enrichMovementWithIcons(Movement movement, Map<Long, WorkspaceCategory> iconMap) {
        // Mapear el movimiento base (sin iconos personalizados)
        MovementRecord baseRecord = movementMapper.toRecord(movement);

        // Si el movimiento no tiene categoría, retornar tal cual
        if (movement.getCategory() == null) {
            return baseRecord;
        }

        // Obtener WorkspaceCategory del map (puede ser null si la categoría fue eliminada del workspace)
        WorkspaceCategory workspaceCategory = iconMap.get(movement.getCategory().getId());

        // Enriquecer la categoría con iconos usando CategoryMapper
        CategoryRecord enrichedCategory = categoryMapper.toRecordWithIcons(
                movement.getCategory(),
                workspaceCategory
        );

        // Crear nuevo record con la categoría enriquecida
        // Orden de campos según MovementRecord:
        // (Long id, BigDecimal amount, String description, LocalDate date, LocalDateTime createdAt,
        //  LocalDateTime updatedAt, CategoryRecord category, CurrencyRecord currency, String bank,
        //  String type, UserBaseRecord owner, AccountBaseRecord account, Integer cuotaActual,
        //  Integer cuotasTotales, BigDecimal exchangeRate, BigDecimal amountUsd)
        return new MovementRecord(
                baseRecord.id(),
                baseRecord.amount(),
                baseRecord.description(),
                baseRecord.date(),
                baseRecord.createdAt(),
                baseRecord.updatedAt(),
                enrichedCategory,  // Categoría enriquecida con iconos
                baseRecord.currency(),
                baseRecord.bank(),
                baseRecord.type(),
                baseRecord.owner(),
                baseRecord.account(),
                baseRecord.cuotaActual(),
                baseRecord.cuotasTotales(),
                baseRecord.exchangeRate(),
                baseRecord.amountUsd()
        );
    }
}
