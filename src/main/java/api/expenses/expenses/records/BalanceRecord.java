package api.expenses.expenses.records;

import api.expenses.expenses.enums.BalanceEnum;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BalanceRecord(String type, Integer year, Integer month, BigDecimal balance, String symbol) {
}
