package api.m2.movements.records.insights;

import api.m2.movements.enums.InsightDirection;

import java.math.BigDecimal;

/**
 * Un "insight" de gasto: indica que el gasto del mes actual en una categoría
 * (para una moneda dada) se desvía significativamente del promedio de los
 * últimos meses.
 */
public record CategoryInsightRecord(
        String category,
        String currency,
        BigDecimal currentAmount,
        BigDecimal averageAmount,
        BigDecimal percentDeviation,
        InsightDirection direction
) {
}
