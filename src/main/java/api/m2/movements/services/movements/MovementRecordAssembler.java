package api.m2.movements.services.movements;

import api.m2.movements.clients.identity.response.UserBaseRecord;
import api.m2.movements.entities.WorkspaceCategory;
import api.m2.movements.entities.movements.Movement;
import api.m2.movements.mappers.CategoryMapper;
import api.m2.movements.mappers.MovementMapper;
import api.m2.movements.records.movements.MovementItemDto;
import api.m2.movements.records.movements.MovementRecord;
import api.m2.movements.records.workspaces.WorkspaceBaseRecord;
import api.m2.movements.repositories.WorkspaceCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Arma el {@link MovementRecord} que se publica por websocket al crear un movimiento. Centraliza
 * lo que {@code MovementMapper.toRecord} no sabe resolver solo: metadata (workspace/owner) y los
 * iconos de las categorías, que viven en {@link WorkspaceCategory} y no en la categoría global.
 */
@Component
@RequiredArgsConstructor
public class MovementRecordAssembler {

    private final MovementMapper movementMapper;
    private final CategoryMapper categoryMapper;
    private final WorkspaceCategoryRepository workspaceCategoryRepository;

    /** Categorías activas del workspace indexadas por id de categoría global. */
    public Map<Long, WorkspaceCategory> iconsOf(Long workspaceId) {
        return workspaceCategoryRepository.findByWorkspaceIdAndIsActiveTrue(workspaceId).stream()
                .collect(Collectors.toMap(wc -> wc.getCategory().getId(), wc -> wc));
    }

    public MovementRecord assemble(Movement movement, WorkspaceBaseRecord workspace,
                                   Map<Long, String> ownerNamesById, List<MovementItemDto> items,
                                   Map<Long, WorkspaceCategory> iconMap) {
        var baseRecord = movementMapper.toRecord(movement);
        // Sin esto el evento de websocket llega con iconName/iconColor en null y el front no
        // dibuja el icono hasta el próximo refresh.
        var categories = movement.getCategories().stream()
                .map(category -> categoryMapper.toRecordWithIcons(category, iconMap.get(category.getId())))
                .toList();
        var metadata = new MovementRecord.Metadata(
                new UserBaseRecord(ownerNamesById.get(movement.getOwnerId()), movement.getOwnerId()),
                workspace, movement.getExchangeRate(), null);
        return new MovementRecord(
                baseRecord.id(), baseRecord.amount(), baseRecord.description(), baseRecord.date(),
                baseRecord.createdAt(), baseRecord.updatedAt(), categories, baseRecord.currency(),
                baseRecord.bank(), baseRecord.type(), baseRecord.cuotaActual(), baseRecord.cuotasTotales(),
                baseRecord.lastCreditPayment(), metadata, items);
    }
}
