package api.m2.movements.records;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record BudgetToAdd(
        @NotNull Long accountId,
        String category,
        @NotBlank String currency,
        @NotNull @Positive BigDecimal amount,
        Integer year,
        Integer month
) {
}
