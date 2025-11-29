package api.expenses.expenses.records;

import java.math.BigDecimal;

public record BalanceRecord(String type, Integer year, Integer month, BigDecimal balance, String symbol) {
}
