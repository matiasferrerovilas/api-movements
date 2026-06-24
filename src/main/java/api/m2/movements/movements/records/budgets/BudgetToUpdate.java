package api.m2.movements.movements.records.budgets;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record BudgetToUpdate(
        @Positive BigDecimal amount
) {
}
