package api.m2.movements.records.goals;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record GoalContribution(
        @NotNull(message = "El monto es requerido")
        @Positive(message = "El monto debe ser mayor a cero")
        BigDecimal amount
) {
}
