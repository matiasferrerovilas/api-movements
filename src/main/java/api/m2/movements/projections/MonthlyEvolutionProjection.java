package api.m2.movements.projections;

import java.math.BigDecimal;

public interface MonthlyEvolutionProjection {
    Integer getMonth();
    String getCurrencySymbol();
    BigDecimal getTotal();
}
