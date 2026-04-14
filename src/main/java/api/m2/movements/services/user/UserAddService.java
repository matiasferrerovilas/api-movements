package api.m2.movements.services.user;

import api.m2.movements.entities.User;
import api.m2.movements.enums.UserType;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.exceptions.PermissionDeniedException;
import api.m2.movements.repositories.UserRepository;
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

    private final UserRepository userRepository;

    @Transactional
    public User createLogInUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            throw new PermissionDeniedException("Usuario no autenticado");
        }

        var jwt = jwtAuth.getToken();
        String email = jwt.getClaimAsString(EMAIL_CLAIM);
        String givenName = jwt.getClaimAsString(GIVEN_NAME_CLAIM);
        String familyName = jwt.getClaimAsString(FAMILY_NAME_CLAIM);

        var user = User.builder()
                .email(email)
                .givenName(givenName)
                .familyName(familyName)
                .isFirstLogin(true)
                .build();

        return userRepository.save(user);
    }

    @Transactional
    public void changeUserFirstLoginStatus(UserType userType, Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario inexistente"));
        user.setFirstLogin(false);
        user.setUserType(userType);
        userRepository.save(user);
    }
}
