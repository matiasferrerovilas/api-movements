package api.m2.movements.services.movements;

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

    @Transactional(readOnly = true)
    public Page<@NonNull MovementRecord> getExpensesBy(MovementSearchFilterRecord filter, Pageable page) {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();
        return movementRepository.getExpenseBy(List.of(workspaceId), filter, page)
                .map(movementMapper::toRecord);
    }
}
