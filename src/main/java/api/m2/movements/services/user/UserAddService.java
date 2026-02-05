package api.m2.movements.services.user;

import api.m2.movements.entities.User;
import api.m2.movements.enums.UserType;
import api.m2.movements.exceptions.PermissionDeniedException;
import api.m2.movements.records.groups.AddGroupRecord;
import api.m2.movements.repositories.UserRepository;
import api.m2.movements.services.accounts.AccountAddService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserAddService {

    private final AccountAddService accountAddService;
    private final UserRepository userRepository;

    public User createLogInUser() {
        String email = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .orElseThrow(() -> new PermissionDeniedException("Usuario no autenticado"));

        var user = User.builder()
                .email(email)
                .isFirstLogin(true)
                .build();

        user = userRepository.save(user);
        accountAddService.createAccount(new AddGroupRecord("DEFAULT"));
        return user;
    }

    public void changeUserFirstLoginStatus(UserType userType, Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario inexistente"));
        user.setFirstLogin(false);
        user.setUserType(userType);
        userRepository.save(user);
    }
}
