package api.m2.movements.records.insights;

import api.m2.movements.enums.InsightDirection;
import api.m2.movements.records.currencies.CurrencyRecord;

import java.math.BigDecimal;

/**
 * Un "insight" de gasto: indica que el gasto del mes actual en una categoría
 * (para una moneda dada) se desvía significativamente del promedio de los
 * últimos meses.
 */
public record CategoryInsightRecord(
        String category,
        CurrencyRecord currency,
        BigDecimal currentAmount,
        BigDecimal averageAmount,
        BigDecimal percentDeviation,
        InsightDirection direction
) {
}
