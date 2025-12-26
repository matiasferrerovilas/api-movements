package api.expenses.expenses.services.user;

import api.expenses.expenses.entities.User;
import api.expenses.expenses.exceptions.PermissionDeniedException;
import api.expenses.expenses.mappers.UserMapper;
import api.expenses.expenses.records.groups.UserRecord;
import api.expenses.expenses.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public User getAuthenticatedUser() {
        String email = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .orElseThrow(() -> new PermissionDeniedException("Usuario no autenticado"));

        return  userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario inexistente"));
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
}

