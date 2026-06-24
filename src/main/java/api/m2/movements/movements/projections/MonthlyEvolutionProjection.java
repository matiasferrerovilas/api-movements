package api.m2.movements.movements.projections;

import java.math.BigDecimal;

public interface MonthlyEvolutionProjection {
    Integer getMonth();
    String getCurrencySymbol();
    BigDecimal getTotal();
}
