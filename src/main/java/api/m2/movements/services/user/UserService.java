package api.m2.movements.services.user;

import api.m2.movements.clients.identity.IdentityClient;
import api.m2.movements.configuration.CacheConfiguration;
import api.m2.movements.exceptions.PermissionDeniedException;
import api.m2.movements.exceptions.ServiceException;
import api.m2.movements.clients.identity.response.UserMe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private static final String USER_NAME_KEY_PREFIX = "user:";

    private final IdentityClient identityClient;
    private final CacheManager cacheManager;

    // Cacheado por 5hs (ver CacheConfiguration.IDENTITY_CACHE) — antes esto disparaba un
    // round-trip HTTP a api-identity en casi cada endpoint de la app, sin cache, sin timeout y
    // sin circuit breaker. Cambios de nombre/tipo de usuario pueden tardar hasta ese tiempo en
    // reflejarse acá a cambio de sacar a api-identity del camino crítico de casi todo el tráfico.
    //
    // La key usa @userService.getAuthenticatedEmail() (referencia a este mismo bean) en vez de
    // T(org.springframework.security.core.context.SecurityContextHolder)...: esta app corre como
    // native image (GraalVM), y T(FQCN) resuelve el tipo vía Class.forName en runtime — un tipo
    // no registrado explícitamente en la reflection config del native image tira "EL1005E: Type
    // cannot be found" aunque esa misma clase esté siendo usada activamente por el resto de la
    // app (Spring Security). Nunca se reproduce en tests porque ahí corre en JVM normal, sin la
    // reflection config cerrada del native image. @bean.metodo() resuelve por BeanFactoryResolver,
    // no por Class.forName, así que esquiva el problema en vez de requerir registrar el tipo.
    @Cacheable(cacheNames = CacheConfiguration.IDENTITY_CACHE, key = "'me:' + @userService.getAuthenticatedEmail()")
    public UserMe getMe() {
        return identityClient.getMe(null);
    }

    /** Same as {@link #getMe()}, but also resolves the caller's role in {@code workspaceId} — null
     * if the caller isn't a member. api-movements decides which workspace's role to ask for (its
     * own notion of "active workspace"); api-identity has no such notion itself. */
    @Cacheable(cacheNames = CacheConfiguration.IDENTITY_CACHE, key = "'me:' + @userService.getAuthenticatedEmail() + ':' + #workspaceId")
    public UserMe getMe(Long workspaceId) {
        return identityClient.getMe(workspaceId);
    }

    /**
     * Nombre de pila de cada usuario, solo para mostrar. Cacheado por usuario (ver
     * {@link CacheConfiguration#NAMES_CACHE}) y best-effort: si api-identity no responde o
     * devuelve un error de servidor, devuelve lo que haya en cache y omite el resto (con un
     * warning) en vez de hacer fallar la operación que lo pidió.
     */
    public Map<Long, String> getUserNamesByIds(List<Long> ids) {
        var cache = cacheManager.getCache(CacheConfiguration.NAMES_CACHE);
        Map<Long, String> names = new HashMap<>();
        List<Long> missing = new ArrayList<>();
        for (Long id : new LinkedHashSet<>(ids)) {
            var cached = cache == null ? null : cache.get(USER_NAME_KEY_PREFIX + id, String.class);
            if (cached != null) {
                names.put(id, cached);
            } else {
                missing.add(id);
            }
        }
        if (!missing.isEmpty()) {
            names.putAll(this.fetchUserNames(missing, cache));
        }
        return names;
    }

    private Map<Long, String> fetchUserNames(List<Long> ids, Cache cache) {
        try {
            Map<Long, String> fetched = new HashMap<>();
            for (UserMe user : identityClient.getUsersByIds(ids)) {
                if (user.givenName() == null) {
                    continue;
                }
                fetched.put(user.id(), user.givenName());
                if (cache != null) {
                    cache.put(USER_NAME_KEY_PREFIX + user.id(), user.givenName());
                }
            }
            return fetched;
        } catch (ResourceAccessException | ServiceException e) {
            log.warn("No se pudieron resolver los nombres de los usuarios {} contra api-identity: {}",
                    ids, e.getMessage());
            return Map.of();
        }
    }

    /** Nombre para mostrar: "Nombre Apellido" si hay, si no el email. Tolera givenName/familyName
     * en null (a diferencia de {@link #getUserNamesByIds}, que rompe con un value null en toMap). */
    public Map<Long, String> getUserDisplayNamesByIds(List<Long> ids) {
        return identityClient.getUsersByIds(ids).stream()
                .collect(Collectors.toMap(UserMe::id, UserService::displayName));
    }

    private static String displayName(UserMe user) {
        String given = user.givenName() != null ? user.givenName() : "";
        String family = user.familyName() != null ? user.familyName() : "";
        String full = (given + " " + family).trim();
        return full.isEmpty() ? user.email() : full;
    }

    public String getAuthenticatedEmail() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .orElseThrow(() -> new PermissionDeniedException("Usuario no autenticado"));
    }

    public String getCurrentKeycloakId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getSubject();
        }

        throw new ServiceException("No hay un JWT autenticado en el contexto de seguridad");
    }
}
