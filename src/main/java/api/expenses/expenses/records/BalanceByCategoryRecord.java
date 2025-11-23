package api.expenses.expenses.records;

import java.math.BigDecimal;

public record BalanceByCategoryRecord(String category, Integer year, String currencySymbol, BigDecimal total) {
}
