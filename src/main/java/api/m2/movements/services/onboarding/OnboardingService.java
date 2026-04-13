package api.m2.movements.services.onboarding;

import api.m2.movements.entities.User;
import api.m2.movements.enums.UserSettingKey;
import api.m2.movements.enums.UserType;
import api.m2.movements.records.currencies.CurrencyRecord;
import api.m2.movements.records.workspaces.AddWorkspaceRecord;
import api.m2.movements.records.income.IncomeToAdd;
import api.m2.movements.records.onboarding.BankToAdd;
import api.m2.movements.records.onboarding.OnBoardingForm;
import api.m2.movements.repositories.WorkspaceRepository;
import api.m2.movements.services.banks.BankAddService;
import api.m2.movements.services.category.UserCategoryService;
import api.m2.movements.services.currencies.CurrencyAddService;
import api.m2.movements.services.workspaces.WorkspaceAddService;
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
    private static final String DEFAULT_WORKSPACE_NAME = "DEFAULT";
    private static final String DEFAULT_CURRENCY = "USD";

    private final UserAddService userAddService;
    private final IncomeAddService incomeAddService;
    private final WorkspaceAddService workspaceAddService;
    private final BankAddService bankAddService;
    private final UserCategoryService userCategoryService;
    private final UserSettingService userSettingService;
    private final CurrencyAddService currencyAddService;
    private final WorkspaceRepository workspaceRepository;

    @Transactional(rollbackFor = Exception.class)
    public void finish(OnBoardingForm onBoardingForm) {
        var user = userAddService.createLogInUser();
        this.createWorkspaces(onBoardingForm, user);
        this.addBanks(onBoardingForm, user);
        this.addDefaultCurrency(user);
        this.addCategories(onBoardingForm, user);
        this.addInitialIncome(onBoardingForm);
        userAddService.changeUserFirstLoginStatus(UserType.valueOf(onBoardingForm.userType()), user.getId());
    }

    private void createWorkspaces(OnBoardingForm onBoardingForm, User user) {
        // 1. Siempre crear DEFAULT primero
        workspaceAddService.createWorkspace(new AddWorkspaceRecord(DEFAULT_WORKSPACE_NAME));

        // 2. Crear los workspaces adicionales (excluyendo DEFAULT si vino en la lista)
        onBoardingForm.accountsToAdd().stream()
                .filter(account -> !DEFAULT_WORKSPACE_NAME.equals(account))
                .forEach(account ->
                        workspaceAddService.createWorkspace(new AddWorkspaceRecord(account)));

        // 3. Setear DEFAULT como workspace por defecto
        workspaceRepository.findWorkspaceByNameAndOwnerId(DEFAULT_WORKSPACE_NAME, user.getId())
                .ifPresent(workspace ->
                        userSettingService.upsertForUser(user, UserSettingKey.DEFAULT_WORKSPACE, workspace.getId()));
    }

    private void addBanks(OnBoardingForm onBoardingForm, User user) {
        List<BankToAdd> banks = onBoardingForm.banksToAdd();
        boolean hasExplicitDefault = banks.stream().anyMatch(BankToAdd::isDefault);
        for (int i = 0; i < banks.size(); i++) {
            var bankToAdd = banks.get(i);
            var bank = bankAddService.addBankToUser(bankToAdd.description(), user);
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
}
