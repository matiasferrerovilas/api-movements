package api.m2.movements.enums;

import java.util.Arrays;

/**
 * Categorías por defecto del sistema.
 * Estas categorías se crean automáticamente durante el onboarding
 * y las que tienen deletable=false no pueden ser eliminadas por el usuario.
 */
public enum DefaultCategory {

    SIN_CATEGORIA("SIN_CATEGORIA", false),
    SERVICIOS("SERVICIOS", false),
    HOGAR("HOGAR", false),
    STREAMING("STREAMING", true);

    private final String description;
    private final boolean deletable;

    DefaultCategory(String description, boolean deletable) {
        this.description = description;
        this.deletable = deletable;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDeletable() {
        return deletable;
    }

    /**
     * Verifica si una descripción corresponde a una categoría por defecto.
     *
     * @param description la descripción a verificar
     * @return true si es una categoría por defecto
     */
    public static boolean isDefault(String description) {
        if (description == null) {
            return false;
        }
        return Arrays.stream(values())
                .anyMatch(cat -> cat.description.equalsIgnoreCase(description.trim()));
    }

    /**
     * Obtiene una categoría por defecto por su descripción.
     *
     * @param description la descripción a buscar
     * @return la categoría por defecto o null si no existe
     */
    public static DefaultCategory fromDescription(String description) {
        if (description == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(cat -> cat.description.equalsIgnoreCase(description.trim()))
                .findFirst()
                .orElse(null);
    }
}
