package api.m2.movements.services.user;

import api.m2.movements.clients.identity.IdentityClient;
import api.m2.movements.configuration.CacheConfiguration;
import api.m2.movements.exceptions.PermissionDeniedException;
import api.m2.movements.exceptions.ServiceException;
import api.m2.movements.clients.identity.response.UserMe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final IdentityClient identityClient;

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

    public Map<Long, String> getUserNamesByIds(List<Long> ids) {
        return identityClient.getUsersByIds(ids).stream()
                .collect(Collectors.toMap(UserMe::id, UserMe::givenName));
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
