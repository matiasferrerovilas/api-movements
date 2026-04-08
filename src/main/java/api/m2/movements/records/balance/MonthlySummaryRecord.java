package api.m2.movements.records.balance;

import java.math.BigDecimal;

public record MonthlySummaryRecord(
        Integer year,
        Integer month,
        BigDecimal totalIngresado,
        BigDecimal totalGastado,
        BigDecimal diferencia,
        String categoriaConMayorGasto,
        MonthlySummaryComparisonRecord comparacionVsMesAnterior
) {
}
