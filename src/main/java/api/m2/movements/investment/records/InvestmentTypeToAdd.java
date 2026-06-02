package api.m2.movements.investment.records;

import api.m2.movements.investment.enums.InvestmentCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InvestmentTypeToAdd(
        @NotBlank(message = "El nombre del tipo de inversión es requerido") String name,
        @NotNull(message = "La categoría es requerida") InvestmentCategory category,
        String iconName,
        String iconColor) {
}
