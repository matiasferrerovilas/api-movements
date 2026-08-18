package api.m2.movements.records.goals;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalToUpdate(
        String name,
        @Positive(message = "El monto objetivo debe ser mayor a cero")
        BigDecimal targetAmount,
        LocalDate targetDate
) {
}
