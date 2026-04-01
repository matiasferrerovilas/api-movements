package api.m2.movements.records.banks;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddBankRequest(
        @NotNull(message = "La descripción no puede ser nula")
        @NotBlank(message = "La descripción no puede estar vacía")
        String description
) {
}
