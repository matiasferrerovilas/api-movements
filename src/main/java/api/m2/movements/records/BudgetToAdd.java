package api.m2.movements.records;

import java.math.BigDecimal;

public record BudgetToAdd(String category, BigDecimal amount, Integer year, Integer month) {
}
