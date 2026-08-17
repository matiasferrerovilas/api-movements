package api.m2.movements.services.income;

import api.m2.movements.mappers.IncomeMapper;
import api.m2.movements.records.income.IncomeRecord;
import api.m2.movements.repositories.IncomeRepository;
import api.m2.movements.services.workspaces.WorkspaceContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncomeQueryService {
    private final IncomeRepository incomeRepository;
    private final WorkspaceContextService workspaceContextService;
    private final IncomeMapper incomeMapper;

    @Transactional(readOnly = true)
    public Page<IncomeRecord> getAllIncomes(Pageable pageable) {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();
        var page = incomeRepository.findAllByWorkspaceId(workspaceId, pageable);
        return new PageImpl<>(incomeMapper.toRecord(page.getContent()), pageable, page.getTotalElements());
    }
}
