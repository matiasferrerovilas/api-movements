package api.m2.movements.records.income;

import api.m2.movements.records.currencies.CurrencyRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(
        description = "Datos necesarios para agregar un ingreso"
)
public record IncomeToAdd(
        @Schema(
                description = "Banco o entidad emisora del ingreso",
                example = "Santander",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "El banco no puede estar vacío")
        String bank,
        @Schema(
                description = "Moneda del ingreso",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "La moneda es requerida")
        @Valid
        CurrencyRecord currency,
        @Schema(
                description = "Monto del ingreso",
                example = "150000.50",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "0.01"
        )
        @NotNull(message = "El monto es requerido")
        @Positive(message = "El monto debe ser mayor a cero")
        BigDecimal amount,
        @Schema(
                description = "Workspace al que pertenece el ingreso",
                example = "DEFAULT",
                allowableValues = {"DEFAULT", "FAMILY"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "El workspace es requerido")
        String workspace) { }
