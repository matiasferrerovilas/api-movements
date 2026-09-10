package api.m2.movements.projections;

import java.math.BigDecimal;

/** Fila cruda de "por usuario y moneda" para el cierre de mes — ver
 * {@code MovementRepository.getUserTotalsByMonth}. */
public interface MonthlyUserCurrencyProjection {
    Long getUserId();

    String getCurrency();

    long getMovementCount();

    BigDecimal getTotalSpent();
}
