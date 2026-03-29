package api.m2.movements.services.user;

import api.m2.movements.entities.User;
import api.m2.movements.enums.UserSettingKey;
import api.m2.movements.enums.UserType;
import api.m2.movements.exceptions.PermissionDeniedException;
import api.m2.movements.records.groups.AddGroupRecord;
import api.m2.movements.repositories.AccountRepository;
import api.m2.movements.repositories.CurrencyRepository;
import api.m2.movements.repositories.UserRepository;
import api.m2.movements.services.groups.GroupAddService;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.services.settings.UserSettingService;
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

    private final GroupAddService groupAddService;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CurrencyRepository currencyRepository;
    private final UserSettingService userSettingService;

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
        groupAddService.createAccount(new AddGroupRecord("DEFAULT"));

        createDefaultSettings(user);

        return user;
    }

    public void changeUserFirstLoginStatus(UserType userType, Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario inexistente"));
        user.setFirstLogin(false);
        user.setUserType(userType);
        userRepository.save(user);
    }

    private void createDefaultSettings(User user) {
        accountRepository.findAccountByNameAndOwnerId("DEFAULT", user.getId())
                .ifPresent(account ->
                        userSettingService.upsertForUser(user, UserSettingKey.DEFAULT_ACCOUNT, account.getId()));

        currencyRepository.findBySymbol("ARS")
                .ifPresent(currency ->
                        userSettingService.upsertForUser(user, UserSettingKey.DEFAULT_CURRENCY, currency.getId()));
    }
}
