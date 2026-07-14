package api.m2.movements.records.currencies;

import jakarta.validation.constraints.NotBlank;

public record CurrencyRecord(
        @NotBlank(message = "El símbolo de la moneda es requerido")
        String symbol,
        Long id) { }
