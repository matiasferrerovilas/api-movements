package api.m2.movements.records.balance;

import java.math.BigDecimal;

public record MonthlySummaryComparisonRecord(
        BigDecimal totalIngresadoMesAnterior,
        BigDecimal totalGastadoMesAnterior,
        BigDecimal diferenciaGasto,
        BigDecimal diferenciaIngreso
) {
}
