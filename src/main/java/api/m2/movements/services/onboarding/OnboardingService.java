package api.m2.movements.services.onboarding;

import api.m2.movements.entities.User;
import api.m2.movements.enums.UserSettingKey;
import api.m2.movements.enums.UserType;
import api.m2.movements.records.currencies.CurrencyRecord;
import api.m2.movements.records.groups.AddGroupRecord;
import api.m2.movements.records.income.IncomeToAdd;
import api.m2.movements.records.onboarding.BankToAdd;
import api.m2.movements.records.onboarding.OnBoardingForm;
import api.m2.movements.services.banks.BankService;
import api.m2.movements.services.category.UserCategoryService;
import api.m2.movements.services.currencies.CurrencyAddService;
import api.m2.movements.services.groups.GroupAddService;
import api.m2.movements.services.income.IncomeAddService;
import api.m2.movements.services.settings.UserSettingService;
import api.m2.movements.services.user.UserAddService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OnboardingService {
    private final UserAddService userAddService;
    private final IncomeAddService incomeAddService;
    private final GroupAddService groupAddService;
    private final BankService bankService;
    private final UserCategoryService userCategoryService;
    private final UserSettingService userSettingService;
    private final CurrencyAddService currencyAddService;

    @Transactional(rollbackFor = Exception.class)
    public void finish(OnBoardingForm onBoardingForm) {
        var user = userAddService.createLogInUser();
        onBoardingForm.accountsToAdd().forEach(account -> groupAddService.createAccount(new AddGroupRecord(account)));
        this.addBanks(onBoardingForm, user);
        this.addDefaultCurrency(user);
        this.addCategories(onBoardingForm, user);
        this.addInitialIncome(onBoardingForm);
        userAddService.changeUserFirstLoginStatus(UserType.valueOf(onBoardingForm.userType()), user.getId());
    }

    private void addBanks(OnBoardingForm onBoardingForm, User user) {
        List<BankToAdd> banks = onBoardingForm.banksToAdd();
        boolean hasExplicitDefault = banks.stream().anyMatch(BankToAdd::isDefault);
        for (int i = 0; i < banks.size(); i++) {
            var bankToAdd = banks.get(i);
            var bank = bankService.addBankToUser(bankToAdd.description(), user);
            boolean isDefault = hasExplicitDefault ? bankToAdd.isDefault() : i == 0;
            if (isDefault) {
                userSettingService.upsertForUser(user, UserSettingKey.DEFAULT_BANK, bank.getId());
            }
        }
    }

    private void addDefaultCurrency(User user) {
        var usd = currencyAddService.findBySymbol(DEFAULT_CURRENCY);
        userSettingService.upsertForUser(user, UserSettingKey.DEFAULT_CURRENCY, usd.getId());
    }

    private void addCategories(OnBoardingForm onBoardingForm, User user) {
        userCategoryService.addCategories(user, onBoardingForm.categoriesToAdd());
        userCategoryService.addDefaultCategories(user);
    }

    private void addInitialIncome(OnBoardingForm onBoardingForm) {
        if (onBoardingForm.onBoardingAmount().bank() != null
                && onBoardingForm.onBoardingAmount().currency() != null
                && onBoardingForm.onBoardingAmount().amount() != null) {
            incomeAddService.loadIncome(new IncomeToAdd(onBoardingForm.onBoardingAmount().bank(),
                    new CurrencyRecord(onBoardingForm.onBoardingAmount().currency(), null),
                    onBoardingForm.onBoardingAmount().amount(),
                    onBoardingForm.onBoardingAmount().accountToAdd()));
        }
    }

    private static final String DEFAULT_CURRENCY = "USD";
}
