package api.m2.movements.records.movements;

import api.m2.movements.records.categories.CategoryUpdateRecord;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExpenseToUpdate(
        @Positive(message = "El monto debe ser mayor a cero")
        BigDecimal amount,
        LocalDate date,
        String description,
        List<CategoryUpdateRecord> categories,
        String currency,
        Integer cuotaActual,
        Integer cuotasTotales,
        String bank
) { }
