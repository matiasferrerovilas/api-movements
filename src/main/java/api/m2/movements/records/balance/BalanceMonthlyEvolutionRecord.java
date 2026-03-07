package api.m2.movements.records.balance;

import java.math.BigDecimal;

public record BalanceMonthlyEvolutionRecord(
        Integer month,
        String currencySymbol,
        BigDecimal total
) {}
