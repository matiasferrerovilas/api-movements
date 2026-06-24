package api.m2.movements.investment.services;

import api.m2.movements.investment.mappers.InvestmentMapper;
import api.m2.movements.investment.records.InvestmentRecord;
import api.m2.movements.investment.repositories.InvestmentRepository;
import api.m2.movements.movements.services.workspaces.WorkspaceContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InvestmentQueryService {

    private final InvestmentRepository investmentRepository;
    private final InvestmentMapper investmentMapper;
    private final WorkspaceContextService workspaceContextService;

    public List<InvestmentRecord> getByWorkspace() {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();
        return investmentRepository.findByWorkspaceId(workspaceId).stream()
                .map(investmentMapper::toRecord)
                .toList();
    }
}
