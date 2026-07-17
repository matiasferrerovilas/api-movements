package api.m2.movements.services.movements;

import api.m2.movements.entities.movements.Movement;
import api.m2.movements.entities.WorkspaceCategory;
import api.m2.movements.mappers.CategoryMapper;
import api.m2.movements.mappers.MovementMapper;
import api.m2.movements.records.categories.CategoryRecord;
import api.m2.movements.records.movements.MovementRecord;
import api.m2.movements.records.movements.MovementSearchFilterRecord;
import api.m2.movements.records.users.UserBaseRecord;
import api.m2.movements.records.workspaces.WorkspaceBaseRecord;
import api.m2.movements.repositories.MovementRepository;
import api.m2.movements.repositories.WorkspaceCategoryRepository;
import api.m2.movements.services.user.UserService;
import api.m2.movements.services.workspaces.WorkspaceContextService;
import api.m2.movements.services.workspaces.WorkspaceQueryService;
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
    private final WorkspaceQueryService workspaceQueryService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public Page<@NonNull MovementRecord> getExpensesBy(MovementSearchFilterRecord filter, Pageable page) {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();

        Page<Movement> movements = movementRepository.getExpenseBy(List.of(workspaceId), filter, page);

        if (movements.isEmpty()) {
            return movements.map(movementMapper::toRecord);
        }

        List<WorkspaceCategory> categories = workspaceCategoryRepository
                .findByWorkspaceIdAndIsActiveTrue(workspaceId);
        Map<Long, WorkspaceCategory> iconMap = categories.stream()
                .collect(Collectors.toMap(wc -> wc.getCategory().getId(), wc -> wc));

        var workspace = new WorkspaceBaseRecord(workspaceId, workspaceQueryService.findWorkspaceNameById(workspaceId));
        var ownerIds = movements.getContent().stream().map(Movement::getOwnerId).distinct().toList();
        var ownerNamesById = userService.getUserNamesByIds(ownerIds);

        return movements.map(movement -> this.enrichMovementWithIcons(movement, iconMap, workspace, ownerNamesById));
    }

    private MovementRecord enrichMovementWithIcons(Movement movement, Map<Long, WorkspaceCategory> iconMap,
                                                     WorkspaceBaseRecord workspace, Map<Long, String> ownerNamesById) {
        MovementRecord baseRecord = movementMapper.toRecord(movement);
        var metadata = this.buildMetadata(movement, workspace, ownerNamesById);

        if (movement.getCategory() == null) {
            return this.withMetadata(baseRecord, metadata);
        }

        WorkspaceCategory workspaceCategory = iconMap.get(movement.getCategory().getId());
        CategoryRecord enrichedCategory = categoryMapper.toRecordWithIcons(movement.getCategory(), workspaceCategory);

        return new MovementRecord(
                baseRecord.id(), baseRecord.amount(), baseRecord.description(), baseRecord.date(),
                baseRecord.createdAt(), baseRecord.updatedAt(), enrichedCategory, baseRecord.currency(),
                baseRecord.bank(), baseRecord.type(), baseRecord.cuotaActual(), baseRecord.cuotasTotales(), metadata);
    }

    private MovementRecord withMetadata(MovementRecord record, MovementRecord.Metadata metadata) {
        return new MovementRecord(
                record.id(), record.amount(), record.description(), record.date(), record.createdAt(),
                record.updatedAt(), record.category(), record.currency(), record.bank(), record.type(),
                record.cuotaActual(), record.cuotasTotales(), metadata);
    }

    private MovementRecord.Metadata buildMetadata(Movement movement, WorkspaceBaseRecord workspace,
                                                    Map<Long, String> ownerNamesById) {
        return new MovementRecord.Metadata(
                new UserBaseRecord(ownerNamesById.get(movement.getOwnerId()), movement.getOwnerId()),
                workspace, movement.getExchangeRate(), null);
    }
}
