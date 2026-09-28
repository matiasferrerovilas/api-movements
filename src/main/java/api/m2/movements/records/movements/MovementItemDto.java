package api.m2.movements.records.movements;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Línea de desglose de un movimiento (ej: "1kg papa", "$500"). {@code price} es el precio total
 * de la línea, no un precio unitario — no se deriva de {@code quantity}.
 */
public record MovementItemDto(
        Long id,
        @NotNull(message = "La cantidad es requerida")
        @Positive(message = "La cantidad debe ser mayor a cero")
        BigDecimal quantity,
        @NotBlank(message = "Debe indicar una unidad")
        String unit,
        @NotBlank(message = "Debe indicar una descripción")
        String description,
        @NotNull(message = "El precio es requerido")
        @PositiveOrZero(message = "El precio no puede ser negativo")
        BigDecimal price
) { }
