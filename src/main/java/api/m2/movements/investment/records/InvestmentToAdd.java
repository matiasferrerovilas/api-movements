package api.m2.movements.investment.records;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvestmentToAdd(
        @NotNull(message = "El monto es requerido")
        @Positive(message = "El monto debe ser mayor a cero")
        BigDecimal amount,
        @NotNull(message = "La fecha de inicio es requerida") LocalDate startDate,
        LocalDate endDate,
        String description,
        String symbol,
        BigDecimal tna,
        @NotNull(message = "El tipo de inversión es requerido") Long investmentTypeId,
        @NotBlank(message = "La moneda es requerida") String currencySymbol) {
}
