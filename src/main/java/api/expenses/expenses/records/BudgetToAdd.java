package api.expenses.expenses.records;

import java.math.BigDecimal;

public record BudgetToAdd(String category, BigDecimal amount, Integer year, Integer month) {
}
