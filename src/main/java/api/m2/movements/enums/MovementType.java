package api.m2.movements.enums;

import java.util.List;

public enum MovementType {
    DEBITO,
    CREDITO,
    INGRESO,
    REINTEGRO;

    public static final List<String> INGRESO_TYPES = List.of(INGRESO.name());
    // El gasto incluye compras en cuotas de tarjeta (CREDITO), no solo débito directo.
    public static final List<String> GASTO_TYPES = List.of(DEBITO.name(), CREDITO.name());
    // Un REINTEGRO resta del gasto (no suma al ingreso): un reembolso reduce lo que realmente
    // gastaste, no es plata nueva entrando.
    public static final List<String> REINTEGRO_TYPES = List.of(REINTEGRO.name());

    /** INGRESO y REINTEGRO son plata que entra: nunca cuentan como gasto. */
    public static boolean isMoneyIn(String type) {
        return INGRESO.name().equals(type) || REINTEGRO.name().equals(type);
    }
}
