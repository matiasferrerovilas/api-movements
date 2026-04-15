package api.m2.movements.records;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
        @NotNull(message = "El año es requerido")
        @Min(value = 2020, message = "El año debe ser mayor o igual a 2020")
        @Max(value = 2100, message = "El año debe ser menor o igual a 2100")
        Integer year,
        @NotNull(message = "El mes es requerido")
        @Min(value = 1, message = "El mes debe estar entre 1 y 12")
        @Max(value = 12, message = "El mes debe estar entre 1 y 12")
        Integer month
) {
}
