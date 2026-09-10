package api.m2.movements.records.balance;

import java.math.BigDecimal;
import java.util.List;

/**
 * Un miembro del workspace y lo que cargó ese mes, desglosado por moneda. Solo aparecen los
 * usuarios con al menos un movimiento (los demás no generan filas en la consulta).
 */
public record MonthlySummaryUserRecord(
        Long userId,
        String name,
        List<PerCurrency> perCurrency
) {
    public record PerCurrency(
            String currency,
            long movementCount,
            BigDecimal totalSpent
    ) {
    }
}
