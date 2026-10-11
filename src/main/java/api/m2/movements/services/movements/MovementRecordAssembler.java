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
import api.m2.movements.services.user.UserService;
import api.m2.movements.services.workspaces.WorkspaceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Arma el {@link MovementRecord} que se devuelve y se publica por websocket. Centraliza lo que
 * {@code MovementMapper.toRecord} no sabe resolver solo: la metadata (workspace/owner, que salen
 * de api-identity) y los iconos de las categorías, que viven en {@link WorkspaceCategory} y no en
 * la categoría global. Lo que es común a varios movimientos del mismo workspace se resuelve una
 * sola vez en un {@link Context}.
 */
@Component
@RequiredArgsConstructor
public class MovementRecordAssembler {

    private final MovementMapper movementMapper;
    private final CategoryMapper categoryMapper;
    private final WorkspaceCategoryRepository workspaceCategoryRepository;
    private final WorkspaceQueryService workspaceQueryService;
    private final UserService userService;

    /** Lo compartido por los movimientos de un mismo workspace al armar sus records. */
    public record Context(WorkspaceBaseRecord workspace, Map<Long, String> ownerNamesById,
                          Map<Long, WorkspaceCategory> iconsByCategoryId) {
    }

    /** Resuelve nombre de workspace y de owners contra api-identity: requiere un JWT de usuario. */
    public Context contextFor(Long workspaceId, Collection<Movement> movements) {
        var workspace = new WorkspaceBaseRecord(workspaceId, workspaceQueryService.findWorkspaceNameById(workspaceId));
        var ownerIds = movements.stream().map(Movement::getOwnerId).distinct().toList();
        return new Context(workspace, userService.getUserNamesByIds(ownerIds), this.iconsOf(workspaceId));
    }

    /** Para jobs sin JWT: omite nombre de workspace y de owners en vez de fallar. */
    public Context contextWithoutIdentity(Long workspaceId) {
        return new Context(new WorkspaceBaseRecord(workspaceId, null), Map.of(), this.iconsOf(workspaceId));
    }

    public MovementRecord assemble(Movement movement, Context context, List<MovementItemDto> items) {
        var baseRecord = movementMapper.toRecord(movement);
        // Sin esto el evento de websocket llega con iconName/iconColor en null y el front no
        // dibuja el icono hasta el próximo refresh.
        var categories = movement.getCategories().stream()
                .map(category -> categoryMapper.toRecordWithIcons(
                        category, context.iconsByCategoryId().get(category.getId())))
                .toList();
        var metadata = new MovementRecord.Metadata(
                new UserBaseRecord(context.ownerNamesById().get(movement.getOwnerId()), movement.getOwnerId()),
                context.workspace(), movement.getExchangeRate(), null);
        return new MovementRecord(
                baseRecord.id(), baseRecord.amount(), baseRecord.description(), baseRecord.date(),
                baseRecord.createdAt(), baseRecord.updatedAt(), categories, baseRecord.currency(),
                baseRecord.bank(), baseRecord.type(), baseRecord.cuotaActual(), baseRecord.cuotasTotales(),
                baseRecord.lastCreditPayment(), metadata, items);
    }

    private Map<Long, WorkspaceCategory> iconsOf(Long workspaceId) {
        return workspaceCategoryRepository.findByWorkspaceIdAndIsActiveTrue(workspaceId).stream()
                .collect(Collectors.toMap(wc -> wc.getCategory().getId(), wc -> wc));
    }
}
