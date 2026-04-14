package api.m2.movements.services.budgets;

import api.m2.movements.mappers.BudgetMapper;
import api.m2.movements.records.budgets.BudgetRecord;
import api.m2.movements.repositories.BudgetRepository;
import api.m2.movements.services.workspaces.WorkspaceContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BudgetQueryService {

    private final BudgetRepository budgetRepository;
    private final BudgetMapper budgetMapper;
    private final WorkspaceContextService workspaceContextService;

    @Transactional(readOnly = true)
    public List<BudgetRecord> getByAccount(String currencySymbol, int year, int month) {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();

        return budgetRepository.findByAccountAndPeriod(workspaceId, currencySymbol, year, month)
                .stream()
                .map(budget -> {
                    BigDecimal spent = this.resolveSpent(budget.getWorkspace().getId(),
                            budget.getCategory() == null ? null : budget.getCategory().getDescription(),
                            budget.getCurrency().getSymbol(),
                            year,
                            month);
                    return budgetMapper.toRecordWithSpent(budget, spent);
                })
                .toList();
    }

    private BigDecimal resolveSpent(Long workspaceId, String categoryDescription,
                                    String currencySymbol, int year, int month) {
        if (categoryDescription == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal result = budgetRepository.sumSpentByCategoryAndPeriod(
                workspaceId, categoryDescription, currencySymbol, year, month);
        return result != null ? result : BigDecimal.ZERO;
    }
}
