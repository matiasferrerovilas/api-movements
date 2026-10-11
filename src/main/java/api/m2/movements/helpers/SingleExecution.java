package api.m2.movements.helpers;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Evita que dos ejecuciones del mismo job corran a la vez (el cron más un click manual del panel
 * admin, o un doble click): la segunda leería el estado antes de que la primera termine de
 * insertar y duplicaría lo que esta aún no registró.
 *
 * shortcut: solo protege dentro de una instancia de la app, alcanza mientras corra una sola;
 * con varias réplicas habría que pasar a un lock en la base.
 */
public final class SingleExecution {

    private final AtomicBoolean running = new AtomicBoolean(false);

    /** Corre la tarea salvo que ya haya otra en curso; devuelve {@code false} si la salteó. */
    public boolean runIfIdle(Runnable task) {
        if (!running.compareAndSet(false, true)) {
            return false;
        }
        try {
            task.run();
            return true;
        } finally {
            running.set(false);
        }
    }
}
