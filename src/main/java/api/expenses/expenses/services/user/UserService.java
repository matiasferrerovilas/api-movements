package api.expenses.expenses.services.user;

import api.expenses.expenses.entities.User;
import api.expenses.expenses.enums.UserType;
import api.expenses.expenses.exceptions.PermissionDeniedException;
import api.expenses.expenses.mappers.UserMapper;
import api.expenses.expenses.records.groups.UserRecord;
import api.expenses.expenses.repositories.UserRepository;
import api.expenses.expenses.services.groups.DefaultGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final DefaultGroupService defaultGroupService;

    public User getAuthenticatedUser() {
        String email = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .orElseThrow(() -> new PermissionDeniedException("Usuario no autenticado"));

        return this.loadOrCreateUser(email);
    }

    public UserRecord getAuthenticatedUserRecord() {
        return userMapper.toRecord(getAuthenticatedUser());
    }

    public List<User> getUserByEmail(List<String> email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findUserByEmail() {
        String email = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .orElseThrow(() -> new PermissionDeniedException("Usuario no autenticado"));

        return userRepository.findByEmail(email);
    }

    public void changeUserFirstLoginStatus(UserType userType) {
        var user = getAuthenticatedUser();
        user.setFirstLogin(false);
        user.setUserType(userType);
        userRepository.save(user);
    }

    public User loadOrCreateUser(String email) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    try {
                        return this.createNewUser(email);
                    } catch (DataIntegrityViolationException e) {
                        return userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("""
                                        Fallo en la condición de carrera:
                                        El usuario no pudo ser creado ni encontrado después del reintento
                                        para el email: %s
                                        """.formatted(email)));

                    }
                });
    }

    private User createNewUser(String email) {
        var defaultGroup = defaultGroupService.getDefaultGroup();

        var user = User.builder()
                .email(email)
                .userGroups(new HashSet<>(List.of(defaultGroup)))
                .isFirstLogin(false)
                .build();

        return userRepository.save(user);
    }
}

