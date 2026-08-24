package api.m2.movements.records.goals;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * name/targetAmount son parciales: null significa "no tocar este campo". targetDate es la
 * excepción — siempre se aplica tal cual viene, así que null la borra explícitamente en vez de
 * dejarla como estaba.
 */
public record GoalToUpdate(
        String name,
        @Positive(message = "El monto objetivo debe ser mayor a cero")
        BigDecimal targetAmount,
        LocalDate targetDate
) {
}
