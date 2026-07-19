package api.m2.movements.services.user;

import api.m2.movements.clients.identity.IdentityClient;
import api.m2.movements.exceptions.PermissionDeniedException;
import api.m2.movements.exceptions.ServiceException;
import api.m2.movements.clients.identity.response.UserMe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public UserMe getMe() {
        return identityClient.getMe();
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
