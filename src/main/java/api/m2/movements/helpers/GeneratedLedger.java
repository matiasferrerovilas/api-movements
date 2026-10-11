package api.m2.movements.helpers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Registro de lo que un job ya generó en un período, para que volver a correrlo no duplique.
 * Cuenta ocurrencias en vez de usar un set: dos elementos idénticos (dos ingresos recurrentes
 * iguales, dos compras en cuotas iguales) necesitan dos movimientos, así que cada coincidencia
 * existente se consume una sola vez.
 */
public final class GeneratedLedger<K> {

    private final Map<K, Integer> remaining = new HashMap<>();

    public GeneratedLedger(Collection<K> existing) {
        existing.forEach(key -> remaining.merge(key, 1, Integer::sum));
    }

    /** {@code true} si ya existía (y lo marca como usado), {@code false} si hay que generarlo. */
    public boolean consume(K key) {
        Integer count = remaining.get(key);
        if (count == null) {
            return false;
        }
        if (count == 1) {
            remaining.remove(key);
        } else {
            remaining.put(key, count - 1);
        }
        return true;
    }
}
