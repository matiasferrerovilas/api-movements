package api.expenses.expenses.services.onboarding;

import api.expenses.expenses.enums.UserType;
import api.expenses.expenses.records.groups.AddGroupRecord;
import api.expenses.expenses.records.income.IncomeToAdd;
import api.expenses.expenses.records.onboarding.OnBoardingForm;
import api.expenses.expenses.services.accounts.AccountAddService;
import api.expenses.expenses.services.income.IncomeAddService;
import api.expenses.expenses.services.user.UserAddService;
import api.expenses.expenses.services.user.UserService;
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

        if (onBoardingForm.onBoardingAmount().bank() != null && onBoardingForm.onBoardingAmount().currency() != null && onBoardingForm.onBoardingAmount().amount() != null) {
            incomeAddService.loadIncome(new IncomeToAdd(onBoardingForm.onBoardingAmount().bank(),
                    onBoardingForm.onBoardingAmount().currency(),
                    onBoardingForm.onBoardingAmount().amount(),
                    onBoardingForm.onBoardingAmount().accountToAdd()));
        }

        userAddService.changeUserFirstLoginStatus(UserType.valueOf(onBoardingForm.userType()), user.getId());
    }
}