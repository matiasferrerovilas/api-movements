package api.m2.movements.records.balance;

import java.math.BigDecimal;

public record MonthlySummaryUnifiedRecord(
        BigDecimal totalIncome,
        BigDecimal totalSpent,
        BigDecimal totalSpentDebit,
        BigDecimal totalSpentCredit,
        BigDecimal net,
        MonthlySummaryComparisonRecord vsPreviousMonth
) {
}
