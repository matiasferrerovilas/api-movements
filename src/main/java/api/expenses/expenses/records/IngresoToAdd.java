package api.expenses.expenses.records;

import java.math.BigDecimal;

public record IngresoToAdd(String bank,
                           String currency,
                           BigDecimal amount,
                           String group) {
}
