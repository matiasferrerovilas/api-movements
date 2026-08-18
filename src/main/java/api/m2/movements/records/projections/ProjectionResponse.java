package api.m2.movements.records.projections;

import java.math.BigDecimal;
import java.util.List;

/**
 * Proyección conservadora de flujo de caja: extrapola linealmente hacia adelante usando
 * el ahorro neto promedio (ingresos - gastos) de los últimos {@code trailingMonths} meses
 * cerrados. Es una estimación simple de tendencia, no incorpora rendimientos de inversión
 * ni ningún otro supuesto especulativo. Los montos están expresados en {@code currency}
 * (la moneda por defecto del usuario, o USD si no tiene una configurada o la tasa de cambio
 * no estuvo disponible al momento del cálculo). Los movimientos que ya están en esa moneda
 * se suman directo, sin conversión; solo los que están en otra moneda pasan por el pivote USD.
 */
public record ProjectionResponse(
        BigDecimal currentBalance,
        BigDecimal averageMonthlyNet,
        Integer trailingMonths,
        String currency,
        List<ProjectedPointRecord> projectedPoints
) {
}
