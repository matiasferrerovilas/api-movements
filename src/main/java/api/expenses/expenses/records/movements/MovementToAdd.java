package api.expenses.expenses.records.movements;

import api.expenses.expenses.constrains.ValidCuotas;
import api.expenses.expenses.enums.BanksEnum;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

@ValidCuotas
public record MovementToAdd(
        @NotNull BigDecimal amount,
        @NotNull(message = "Fecha no puede ser nula") LocalDate date,
        @NotNull(message = "Debe indicar una descripción") String description,
        String category,
        @NotNull(message = "Debe indicar un tipo de gasto") String type,
        @NotNull(message = "Debe indicar un tipo de moneda")  String currency,
        Integer cuotaActual,
        Integer cuotasTotales,
        BanksEnum bank,
        Long accountId
) { }