package api.m2.movements.records.balance;

import java.math.BigDecimal;
import java.util.List;

public record MonthlySummaryByCurrencyRecord(
        String currency,
        long movementCount,
        BigDecimal totalIncome,
        BigDecimal totalSpent,
        BigDecimal totalSpentDebit,
        BigDecimal totalSpentCredit,
        BigDecimal net,
        String topSpendingCategory,
        MonthlySummaryComparisonRecord vsPreviousMonth,
        List<CategoryAmountRecord> spendingByCategory
) {
}
