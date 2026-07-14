package api.m2.movements.records.balance;

import java.math.BigDecimal;

public record MonthlySummaryByCurrencyRecord(
        String currency,
        BigDecimal totalIngresado,
        BigDecimal totalGastado,
        BigDecimal diferencia,
        String categoriaConMayorGasto,
        MonthlySummaryComparisonRecord comparacionVsMesAnterior
) {
}
