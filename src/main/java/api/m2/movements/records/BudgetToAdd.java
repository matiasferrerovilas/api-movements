package api.m2.movements.records;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record BudgetToAdd(
        @NotBlank(message = "La categoría es requerida")
        String category,
        @NotBlank(message = "La moneda es requerida")
        String currency,
        @NotNull(message = "El monto es requerido")
        @Positive(message = "El monto debe ser mayor a cero")
        BigDecimal amount,
        Integer year,
        Integer month
) {
}
