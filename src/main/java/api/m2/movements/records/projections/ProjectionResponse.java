package api.m2.movements.records.projections;

import java.math.BigDecimal;
import java.util.List;

/**
 * Proyección conservadora de flujo de caja: extrapola linealmente hacia adelante usando
 * el ahorro neto promedio (ingresos - gastos) de los últimos {@code trailingMonths} meses
 * cerrados. Es una estimación simple de tendencia, no incorpora rendimientos de inversión
 * ni ningún otro supuesto especulativo.
 */
public record ProjectionResponse(
        BigDecimal currentBalance,
        BigDecimal averageMonthlyNet,
        Integer trailingMonths,
        List<ProjectedPointRecord> projectedPoints
) {
}
