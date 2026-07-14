package api.m2.movements.records.balance;

import java.math.BigDecimal;

public record MonthlySummaryUnifiedRecord(
        BigDecimal totalIngresado,
        BigDecimal totalGastado,
        BigDecimal diferencia,
        MonthlySummaryComparisonRecord comparacionVsMesAnterior
) {
}
