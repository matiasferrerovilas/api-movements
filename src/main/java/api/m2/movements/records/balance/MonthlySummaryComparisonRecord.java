package api.m2.movements.records.balance;

import java.math.BigDecimal;

public record MonthlySummaryComparisonRecord(
        BigDecimal previousMonthIncome,
        BigDecimal previousMonthSpent,
        BigDecimal spentDelta,
        BigDecimal incomeDelta
) {
}
