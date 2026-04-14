package api.m2.movements.services.user;

import api.m2.movements.entities.User;
import api.m2.movements.enums.UserSettingKey;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.exceptions.PermissionDeniedException;
import api.m2.movements.mappers.UserMapper;
import api.m2.movements.records.users.UserBaseRecord;
import api.m2.movements.records.users.UserMeRecord;
import api.m2.movements.repositories.UserRepository;
import api.m2.movements.repositories.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserSettingRepository userSettingRepository;

    @Transactional(readOnly = true)
    public User getAuthenticatedUser() {
        String email = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .orElseThrow(() -> new PermissionDeniedException("Usuario no autenticado"));

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario inexistente"));
    }

    @Transactional(readOnly = true)
    public UserBaseRecord getAuthenticatedUserRecord() {
        return userMapper.toRecord(getAuthenticatedUser());
    }

    @Transactional(readOnly = true)
    public List<User> getUserByEmail(List<String> email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public Optional<User> findUserByEmail() {
        String email = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .orElseThrow(() -> new PermissionDeniedException("Usuario no autenticado"));

        return userRepository.findByEmail(email);
    }

    public String getCurrentKeycloakId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getSubject();
        }

        throw new IllegalStateException("No hay un JWT autenticado en el contexto de seguridad");
    }

    @Transactional(readOnly = true)
    public UserMeRecord getMe() {
        var optional = findUserByEmail();
        if (optional.isEmpty()) {
            return new UserMeRecord(null, null, null, null, true, null, false);
        }
        var user = optional.get();
        return new UserMeRecord(
                user.getId(),
                user.getEmail(),
                user.getGivenName(),
                user.getFamilyName(),
                user.isFirstLogin(),
                user.getUserType() != null ? user.getUserType().name() : null,
                user.isHasSeenTour()
        );
    }

    @Transactional(readOnly = true)
    public List<User> getUsersWithMonthlySnapshotEnabled() {
        return userSettingRepository.findUsersWithSettingEnabled(UserSettingKey.MONTHLY_SUMMARY_ENABLED);
    }

    @Transactional(readOnly = true)
    public List<User> getUsersWithAutoIncomeEnabled() {
        return userSettingRepository.findUsersWithSettingEnabled(UserSettingKey.AUTO_INCOME_ENABLED);
    }

    @Transactional
    public void markTourAsSeen() {
        var user = getAuthenticatedUser();
        user.setHasSeenTour(true);
        userRepository.save(user);
    }
}
