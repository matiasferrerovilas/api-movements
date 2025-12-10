package api.expenses.expenses.records.income;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(
        description = "Datos necesarios para agregar un ingreso"
)
public record IngresoToAdd(
        @Schema(
                description = "Banco o entidad emisora del ingreso",
                example = "Santander",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String bank,
        @Schema(
                description = "Moneda del ingreso",
                example = "ARS",
                allowableValues = {"ARS", "USD"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String currency,
        @Schema(
                description = "Monto del ingreso",
                example = "150000.50",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "0.01"
        )
        BigDecimal amount,
        @Schema(
                description = "Grupo al que pertenece el ingreso",
                example = "DEFAULT",
                allowableValues = {"DEFAULT", "FAMILY"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String group) { }
