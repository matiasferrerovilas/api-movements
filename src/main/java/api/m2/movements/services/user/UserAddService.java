package api.m2.movements.services.user;

import api.m2.movements.entities.User;
import api.m2.movements.enums.UserType;
import api.m2.movements.exceptions.PermissionDeniedException;
import api.m2.movements.repositories.UserRepository;
import api.m2.movements.exceptions.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserAddService {

    private final UserRepository userRepository;

    @Transactional
    public User createLogInUser() {
        String email = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .orElseThrow(() -> new PermissionDeniedException("Usuario no autenticado"));

        var user = User.builder()
                .email(email)
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
