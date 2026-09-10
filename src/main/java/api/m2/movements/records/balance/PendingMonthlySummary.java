package api.m2.movements.records.balance;

/**
 * El mes recién cerrado que el usuario todavía no vio en su workspace por defecto. Viaja en
 * {@code GET /v1/users/me} → {@code metadata.pendingMonthlySummary}; null cuando no hay nada
 * pendiente. El frontend lo usa como compuerta: si viene, muestra el flujo de cierre de mes en
 * lugar de cualquier ruta.
 */
public record PendingMonthlySummary(int year, int month) {
}
