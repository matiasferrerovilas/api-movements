package api.m2.movements.services.budgets;

import api.m2.movements.mappers.BudgetMapper;
import api.m2.movements.records.budgets.BudgetRecord;
import api.m2.movements.repositories.BudgetRepository;
import api.m2.movements.services.groups.AccountQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BudgetQueryService {

    private final BudgetRepository budgetRepository;
    private final BudgetMapper budgetMapper;
    private final AccountQueryService accountQueryService;

    public List<BudgetRecord> getByAccount(Long accountId, String currencySymbol, int year, int month) {
        accountQueryService.findAccountById(accountId);

        return budgetRepository.findByAccountAndPeriod(accountId, currencySymbol, year, month)
                .stream()
                .map(budget -> {
                    BigDecimal spent = this.resolveSpent(budget.getAccount().getId(),
                            budget.getCategory() == null ? null : budget.getCategory().getDescription(),
                            budget.getCurrency().getSymbol(),
                            year,
                            month);
                    return budgetMapper.toRecordWithSpent(budget, spent);
                })
                .toList();
    }

    private BigDecimal resolveSpent(Long accountId, String categoryDescription,
                                    String currencySymbol, int year, int month) {
        if (categoryDescription == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal result = budgetRepository.sumSpentByCategoryAndPeriod(
                accountId, categoryDescription, currencySymbol, year, month);
        return result != null ? result : BigDecimal.ZERO;
    }
}
