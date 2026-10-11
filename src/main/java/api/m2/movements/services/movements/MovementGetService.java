package api.m2.movements.services.movements;

import api.m2.movements.entities.movements.Movement;
import api.m2.movements.mappers.MovementMapper;
import api.m2.movements.records.movements.MovementRecord;
import api.m2.movements.records.movements.MovementSearchFilterRecord;
import api.m2.movements.repositories.MovementRepository;
import api.m2.movements.services.workspaces.WorkspaceContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovementGetService {
    private final MovementRepository movementRepository;
    private final MovementMapper movementMapper;
    private final WorkspaceContextService workspaceContextService;
    private final MovementItemService movementItemService;
    private final MovementRecordAssembler assembler;

    @Transactional(readOnly = true)
    public Page<@NonNull MovementRecord> getExpensesBy(MovementSearchFilterRecord filter, Pageable page) {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();

        Page<Movement> movements = movementRepository.getExpenseBy(List.of(workspaceId), filter, page);

        if (movements.isEmpty()) {
            return movements.map(movementMapper::toRecord);
        }

        // getExpenseBy ya trae currency/bank fetch-joined (son *-to-one); categories es
        // @ManyToMany y no se puede fetch-joinear junto con Pageable sin que Hibernate pagine en
        // memoria, así que se hidrata acá con una segunda consulta acotada a esta página — misma
        // sesión, mismas instancias administradas, sin volver a tocar la DB por movimiento cuando
        // el assembler después llame a movement.getCategories().
        movementRepository.findByIdInFetchingCategories(
                movements.getContent().stream().map(Movement::getId).toList());

        var context = assembler.contextFor(workspaceId, movements.getContent());
        var itemsByMovement = movementItemService.getItemsByMovementIds(
                movements.getContent().stream().map(Movement::getId).toList());

        return movements.map(movement -> assembler.assemble(
                movement, context, itemsByMovement.getOrDefault(movement.getId(), List.of())));
    }
}
