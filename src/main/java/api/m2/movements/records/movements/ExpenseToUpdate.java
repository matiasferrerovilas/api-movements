package api.m2.movements.records.movements;

import api.m2.movements.records.categories.CategoryUpdateRecord;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseToUpdate(
        @Positive(message = "El monto debe ser mayor a cero")
        BigDecimal amount,
        LocalDate date,
        String description,
        CategoryUpdateRecord category,
        String currency,
        Integer cuotaActual,
        Integer cuotasTotales,
        String bank
) { }
