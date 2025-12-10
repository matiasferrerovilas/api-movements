package api.expenses.expenses.services.onboarding;

import api.expenses.expenses.entities.User;
import api.expenses.expenses.entities.UserGroups;
import api.expenses.expenses.enums.BanksEnum;
import api.expenses.expenses.enums.CurrencyEnum;
import api.expenses.expenses.enums.GroupsEnum;
import api.expenses.expenses.enums.UserType;
import api.expenses.expenses.exceptions.BusinessException;
import api.expenses.expenses.exceptions.PermissionDeniedException;
import api.expenses.expenses.records.income.IngresoToAdd;
import api.expenses.expenses.records.onboarding.OnBoardingAmount;
import api.expenses.expenses.records.onboarding.OnBoardingForm;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    @Test
    @DisplayName("No Verifica si es primer Login dado qeu el usuario no tiene permisos")
    void isFirstLoginException() {
        when(userService.findUserByEmail())
                .thenThrow(new PermissionDeniedException("Usuario no autenticado"));
        final PermissionDeniedException exception = assertThrows(PermissionDeniedException.class,
                () -> onboardingService.isFirstLogin());

        assertEquals("Usuario no autenticado", exception.getMessage());
    }

    @Test
    @DisplayName("Finalizo el Onboarding de forma correcta")
    void finishOnboardingCorrectly() {

        OnBoardingAmount onBoardingAmount = new OnBoardingAmount(new BigDecimal("1300.0"),
                GroupsEnum.DEFAULT.name());
        OnBoardingForm onBoardingForm = new OnBoardingForm(onBoardingAmount,
                BanksEnum.GALICIA.name(),
                CurrencyEnum.ARS.name(),
                UserType.CONSUMER.name(),
                List.of(BanksEnum.GALICIA.name(), BanksEnum.BANCO_CIUDAD.name()));

        when(defaultGroupService.getDefaultGroup())
                .thenReturn(UserGroups.builder().description(GroupsEnum.DEFAULT.name()).build());

        onboardingService.finish(onBoardingForm);

        verify(userService).getAuthenticatedUser();
        verify(settingService).addIngreso(new IngresoToAdd(onBoardingForm.bank(),
                onBoardingForm.currency(),
                onBoardingForm.onBoardingAmount().amount(),
                GroupsEnum.DEFAULT.name()));
    }

    @Test
    @DisplayName("Falla el guardado del Ingreso asi que no termino el onboarding")
    void finishOnboardingFailIngreso() {

        OnBoardingAmount onBoardingAmount = new OnBoardingAmount(
                new BigDecimal("1300.0"), GroupsEnum.DEFAULT.name());
        OnBoardingForm onBoardingForm = new OnBoardingForm(
                onBoardingAmount,
                BanksEnum.GALICIA.name(),
                CurrencyEnum.ARS.name(),
                UserType.CONSUMER.name(),
                List.of(BanksEnum.GALICIA.name(), BanksEnum.BANCO_CIUDAD.name()));

        when(defaultGroupService.getDefaultGroup())
                .thenReturn(UserGroups.builder()
                        .description(GroupsEnum.DEFAULT.name())
                        .build());

        doThrow(new BusinessException("Invalid payment method"))
                .when(settingService)
                .addIngreso(any(IngresoToAdd.class));

        assertThrows(BusinessException.class,
                () -> onboardingService.finish(onBoardingForm));

        verify(userService).getAuthenticatedUser();
        verify(settingService).addIngreso(any(IngresoToAdd.class));
        verify(userService, never()).changeUserFirstLoginStatus(any());
    }
}