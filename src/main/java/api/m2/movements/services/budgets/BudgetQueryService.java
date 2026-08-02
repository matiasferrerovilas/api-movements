package api.m2.movements.services.budgets;

import api.m2.movements.entities.Budget;
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

        List<Budget> budgets = (currencySymbol == null)
                ? budgetRepository.findByWorkspaceAndPeriod(workspaceId, year, month)
                : budgetRepository.findByWorkspaceCurrencyAndPeriod(workspaceId, currencySymbol, year, month);

        return budgets.stream()
                .map(budget -> {
                    BigDecimal spent = this.resolveSpent(budget, year, month);
                    return budgetMapper.toRecordWithSpent(budget, spent);
                })
                .toList();
    }

    private BigDecimal resolveSpent(Budget budget, int year, int month) {
        String categoryDescription = budget.getCategory() == null ? null : budget.getCategory().getDescription();
        if (categoryDescription == null) {
            return BigDecimal.ZERO;
        }
        String currencySymbol = budget.getCurrency().getSymbol();
        BigDecimal result = (budget.getYear() != null && budget.getMonth() == null)
                ? budgetRepository.sumSpentByCategoryAndYear(
                        budget.getWorkspaceId(), categoryDescription, currencySymbol, budget.getYear())
                : budgetRepository.sumSpentByCategoryAndPeriod(
                        budget.getWorkspaceId(), categoryDescription, currencySymbol, year, month);
        return result != null ? result : BigDecimal.ZERO;
    }
}
