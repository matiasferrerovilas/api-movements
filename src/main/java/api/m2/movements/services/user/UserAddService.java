package api.m2.movements.services.user;

import api.m2.movements.clients.identity.IdentityClient;
import api.m2.movements.clients.identity.requests.UserToAdd;
import api.m2.movements.enums.UserType;
import api.m2.movements.exceptions.PermissionDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserAddService {

    private static final String EMAIL_CLAIM = "email";
    private static final String GIVEN_NAME_CLAIM = "given_name";
    private static final String FAMILY_NAME_CLAIM = "family_name";

    private final IdentityClient identityClient;

    @Transactional
    public UserToAdd createLogInUser(String userType) {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            throw new PermissionDeniedException("Usuario no autenticado");
        }

        var jwt = jwtAuth.getToken();
        String email = jwt.getClaimAsString(EMAIL_CLAIM);
        String givenName = jwt.getClaimAsString(GIVEN_NAME_CLAIM);
        String familyName = jwt.getClaimAsString(FAMILY_NAME_CLAIM);

        var user = UserToAdd.builder()
                .email(email)
                .givenName(givenName)
                .familyName(familyName)
                .isFirstLogin(true)
                .userType(UserType.valueOf(userType))
                .build();

        return identityClient.createLogInUser(user);
    }

    @Transactional
    public void changeUserFirstLoginStatus(Long userId) {
        identityClient.changeUserFirstLoginStatus(userId);
    }
}
