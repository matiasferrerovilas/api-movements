package api.m2.movements.records.currencies;

import jakarta.validation.constraints.NotBlank;

public record CurrencyToAdd(
        @NotBlank(message = "El símbolo es requerido")
        String symbol,

        @NotBlank(message = "La descripción es requerida")
        String description
) {
}
