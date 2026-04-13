package api.m2.movements.records.categories;

/**
 * Record para actualizar la categoría de un movimiento.
 * Solo requiere el id o la descripción para identificar la categoría.
 */
public record CategoryUpdateRecord(Long id, String description) {
}
