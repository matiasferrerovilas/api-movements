package api.expenses.expenses.services.onboarding;

import api.expenses.expenses.records.IngresoToAdd;
import api.expenses.expenses.records.groups.AddGroupRecord;
import api.expenses.expenses.records.onboarding.OnBoardingForm;
import api.expenses.expenses.services.groups.DefaultGroupService;
import api.expenses.expenses.services.groups.GroupAddService;
import api.expenses.expenses.services.settings.SettingService;
import api.expenses.expenses.services.user.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
@RequiredArgsConstructor
public class OnboardingService {
    private final UserService userService;
    private final SettingService settingService;
    private final DefaultGroupService defaultGroupService;
    private final GroupAddService groupAddService;

    public boolean isFirstLogin() {
        var optional = userService.findUserByEmail();

        return optional.isPresent() && optional.get().isFirstLogin();
    }

    @Transactional
    public void finish(OnBoardingForm onBoardingForm) {
        userService.getAuthenticatedUser();
        var ingresoGroup = Optional.ofNullable(onBoardingForm.onBoardingAmount().group())
                .orElse(defaultGroupService.getDefaultGroup().getDescription());

        settingService.addIngreso(new IngresoToAdd(onBoardingForm.bank(), onBoardingForm.currency(), onBoardingForm.onBoardingAmount().amount(), ingresoGroup));

        onBoardingForm.groups().forEach(group -> {
            groupAddService.saveGroup(new AddGroupRecord(group));
        });
        userService.changeUserFirstLoginStatus();
    }
}
