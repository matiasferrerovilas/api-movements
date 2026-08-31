package api.m2.movements.configuration;

import api.m2.movements.exceptions.RateLimitExceededException;
import api.m2.movements.services.ratelimit.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * A diferencia de api-identity, este backend no tenía ningún rate limiting — ni acá (CRUD
 * estándar) ni en el endpoint de importar extracto bancario (que además hace parseo de PDF
 * pesado; ver el límite específico y más estricto en {@code MovementImportFileService}). Este es
 * el piso genérico por usuario autenticado, aplicado a todo {@code /v1/**}: generoso a propósito
 * (uso normal de una SPA con refetch en foco/background no debería acercarse a esto), pensado
 * para cortar un script/loop, no para limitar tráfico legítimo.
 */
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_REQUESTS_PER_WINDOW = 200;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final RateLimiterService rateLimiterService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            // Nada autenticado llega hasta acá en la práctica (la cadena de seguridad ya lo
            // rechaza antes) — se deja pasar en vez de romper con un 500 si de alguna forma pasa.
            return true;
        }

        String key = "rate-limit:http:" + authentication.getName();
        if (!rateLimiterService.tryAcquire(key, MAX_REQUESTS_PER_WINDOW, WINDOW)) {
            throw new RateLimitExceededException("Demasiadas solicitudes. Probá de nuevo en un momento.");
        }

        return true;
    }
}
