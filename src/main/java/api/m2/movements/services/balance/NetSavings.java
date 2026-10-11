package api.m2.movements.services.balance;

import api.m2.movements.enums.MovementType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Ahorro neto promedio de los últimos meses cerrados: ingresos - (gastos - reintegros). Cada
 * caller decide cómo obtiene el total de un mes y de unos tipos (en una moneda nativa, convertido
 * a otra, etc.); la fórmula y el redondeo viven acá para que no diverjan entre pantallas.
 */
public final class NetSavings {

    private static final int SCALE = 2;

    private NetSavings() {
    }

    public static BigDecimal averageMonthly(YearMonth lastClosedMonth, int months,
                                            BiFunction<YearMonth, List<String>, BigDecimal> totalOf) {
        BigDecimal totalNet = BigDecimal.ZERO;
        for (int i = 0; i < months; i++) {
            YearMonth yearMonth = lastClosedMonth.minusMonths(i);
            BigDecimal ingresado = totalOf.apply(yearMonth, MovementType.INGRESO_TYPES);
            BigDecimal gastadoBruto = totalOf.apply(yearMonth, MovementType.GASTO_TYPES);
            BigDecimal reintegrado = totalOf.apply(yearMonth, MovementType.REINTEGRO_TYPES);
            totalNet = totalNet.add(ingresado.subtract(gastadoBruto.subtract(reintegrado)));
        }
        return totalNet.divide(BigDecimal.valueOf(months), SCALE, RoundingMode.HALF_UP);
    }
}
