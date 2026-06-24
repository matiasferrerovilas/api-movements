package api.m2.movements.movements.records.settings;

import jakarta.validation.constraints.NotNull;

public record UserSettingRequest(
        @NotNull(message = "El valor es requerido")
        Long value) {
}
