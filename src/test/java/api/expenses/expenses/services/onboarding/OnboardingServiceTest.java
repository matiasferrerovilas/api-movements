package api.expenses.expenses.services.onboarding;

import api.expenses.expenses.entities.User;
import api.expenses.expenses.services.groups.DefaultGroupService;
import api.expenses.expenses.services.groups.GroupAddService;
import api.expenses.expenses.services.settings.SettingService;
import api.expenses.expenses.services.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {
    @Mock
    private UserService userService;
    @Mock
    private SettingService settingService;
    @Mock
    private DefaultGroupService defaultGroupService;
    @Mock
    private GroupAddService groupAddService;
    @InjectMocks
    private OnboardingService onboardingService;

    @Test
    @DisplayName("Verifica correctamente que es el primer login del usuario, dado que no existe")
    void isFirstLoginUserNotExists() {
        when(userService.findUserByEmail())
                .thenReturn(Optional.empty());
        var result = onboardingService.isFirstLogin();
        assertTrue(result);
    }
    @Test
    @DisplayName("Verifica correctamente que es el primer login del usuario, dado que nunca entro")
    void isFirstLoginUserExists() {
        when(userService.findUserByEmail())
                .thenReturn(Optional.of(User.builder().isFirstLogin(true).build()));
        var result = onboardingService.isFirstLogin();
        assertTrue(result);
    }
}