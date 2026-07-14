package api.m2.movements.records.categories;

import jakarta.validation.constraints.Size;

/**
 * Record para actualizar los datos de una categoría (descripción, ícono, color).
 * Todos los campos son opcionales - solo se actualizan los campos proporcionados.
 */
public record CategoryPatchRequest(
        @Size(min = 1, max = 50, message = "La descripción debe tener entre 1 y 50 caracteres")
        String description,

        @Size(max = 50, message = "El nombre del ícono no puede exceder 50 caracteres")
        String iconName,

        @Size(min = 4, max = 7, message = "El color debe ser un código hexadecimal válido (ej: #faad14)")
        String iconColor
) {
}
