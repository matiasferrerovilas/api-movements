package api.m2.movements.records.investments;

import jakarta.validation.constraints.NotBlank;

public record InvestmentTypeToAdd(
        @NotBlank(message = "El nombre del tipo de inversión es requerido") String name,
        String iconName,
        String iconColor) {
}
