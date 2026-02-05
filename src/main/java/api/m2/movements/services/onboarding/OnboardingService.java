package api.m2.movements.services.onboarding;

import api.m2.movements.enums.UserType;
import api.m2.movements.records.groups.AddGroupRecord;
import api.m2.movements.records.income.IncomeToAdd;
import api.m2.movements.records.onboarding.OnBoardingForm;
import api.m2.movements.services.accounts.AccountAddService;
import api.m2.movements.services.income.IncomeAddService;
import api.m2.movements.services.user.UserAddService;
import api.m2.movements.services.user.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class OnboardingService {
    private final UserAddService userAddService;
    private final IncomeAddService incomeAddService;
    private final AccountAddService accountAddService;
    private final UserService userService;

    public boolean isFirstLogin() {
        var optional = userService.findUserByEmail();

        return optional.isEmpty() || optional.get().isFirstLogin();
    }

    @Transactional(rollbackOn = Exception.class)
    public void finish(OnBoardingForm onBoardingForm) {
        var user = userAddService.createLogInUser();

        onBoardingForm.accountsToAdd().forEach(account -> {
            accountAddService.createAccount(new AddGroupRecord(account));
        });

        if (onBoardingForm.onBoardingAmount().bank() != null
                && onBoardingForm.onBoardingAmount().currency() != null
                && onBoardingForm.onBoardingAmount().amount() != null) {
            incomeAddService.loadIncome(new IncomeToAdd(onBoardingForm.onBoardingAmount().bank(),
                    onBoardingForm.onBoardingAmount().currency(),
                    onBoardingForm.onBoardingAmount().amount(),
                    onBoardingForm.onBoardingAmount().accountToAdd()));
        }

        userAddService.changeUserFirstLoginStatus(UserType.valueOf(onBoardingForm.userType()), user.getId());
    }
}