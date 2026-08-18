package api.m2.movements.records.goals;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalToAdd(
        @NotNull(message = "El workspace es requerido")
        Long workspaceId,
        @NotBlank(message = "El nombre de la meta es requerido")
        String name,
        @NotNull(message = "El monto objetivo es requerido")
        @Positive(message = "El monto objetivo debe ser mayor a cero")
        BigDecimal targetAmount,
        @NotBlank(message = "La moneda es requerida")
        String currency,
        LocalDate targetDate
) {
}
