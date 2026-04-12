package api.m2.movements.records.services;

import api.m2.movements.records.currencies.CurrencyRecord;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionToAdd(
        @NotBlank(message = "La descripción es requerida")
        String description,
        @NotNull(message = "El monto es requerido")
        @Positive(message = "El monto debe ser mayor a cero")
        BigDecimal amount,
        @NotNull(message = "La moneda es requerida")
        @Valid
        CurrencyRecord currency,
        LocalDate lastPayment,
        Boolean isPaid,
        @NotNull(message = "El workspace es requerido")
        Long workspaceId) {
}
