package api.m2.movements.services.workspaces;

import api.m2.movements.configuration.CacheConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Invalida lo que api-movements recuerda de la autorización de api-identity (membership y rol)
 * cuando alguien sale o lo sacan de un workspace. Sin esto, el usuario seguiría pasando por la
 * membership cacheada (5hs) y podría seguir leyendo y escribiendo en un workspace al que ya no
 * pertenece.
 *
 * shortcut: vacía las cachés completas en vez de solo las claves del usuario y workspace
 * afectados, porque el evento de api-identity trae el email pero no el userId que usa la key de
 * membership. Es una operación rara y el costo es una consulta a api-identity por usuario activo;
 * si el volumen creciera habría que agregar el userId al evento y evict por clave.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IdentityCacheInvalidator {

    private static final List<String> AUTHORIZATION_CACHES = List.of(
            CacheConfiguration.IDENTITY_CACHE, CacheConfiguration.WORKSPACE_ROLE_CACHE);

    private final CacheManager cacheManager;

    public void invalidateAuthorization() {
        // invalidate() (no clear()) porque es la variante que espera a que el borrado termine: con
        // Redis, clear() puede completarse después de volver y una request pegada vería el valor viejo.
        AUTHORIZATION_CACHES.stream()
                .map(cacheManager::getCache)
                .filter(Objects::nonNull)
                .forEach(Cache::invalidate);
        log.info("Cachés de autorización invalidadas: {}", AUTHORIZATION_CACHES);
    }
}
