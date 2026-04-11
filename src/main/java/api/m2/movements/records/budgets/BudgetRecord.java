package api.m2.movements.records.budgets;

import api.m2.movements.records.categories.CategoryRecord;
import api.m2.movements.records.currencies.CurrencyRecord;

import java.math.BigDecimal;

public record BudgetRecord(
        Long id,
        Long workspaceId,
        CategoryRecord category,
        CurrencyRecord currency,
        BigDecimal amount,
        Integer year,
        Integer month,
        BigDecimal spent,
        BigDecimal percentage
) {
}
