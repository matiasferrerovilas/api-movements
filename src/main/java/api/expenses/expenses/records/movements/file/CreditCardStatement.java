package api.expenses.expenses.records.movements.file;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreditCardStatement(
        BigDecimal amount,
        LocalDate date,
        String description,
        String category,
        String type,
        String currency,
        Integer cuotaActual,
        Integer cuotasTotales,
        String bank,
        String group
) {}
