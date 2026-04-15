package api.m2.movements.services.onboarding;

import api.m2.movements.entities.User;
import api.m2.movements.entities.Workspace;
import api.m2.movements.enums.UserSettingKey;
import api.m2.movements.enums.UserType;
import api.m2.movements.records.currencies.CurrencyRecord;
import api.m2.movements.records.income.IncomeToAdd;
import api.m2.movements.records.onboarding.BankToAdd;
import api.m2.movements.records.onboarding.OnBoardingForm;
import api.m2.movements.records.workspaces.AddWorkspaceRecord;
import api.m2.movements.repositories.WorkspaceRepository;
import api.m2.movements.services.banks.BankAddService;
import api.m2.movements.services.category.WorkspaceCategoryService;
import api.m2.movements.services.currencies.CurrencyAddService;
import api.m2.movements.services.income.IncomeAddService;
import api.m2.movements.services.settings.UserSettingService;
import api.m2.movements.services.user.UserAddService;
import api.m2.movements.services.workspaces.WorkspaceAddService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingService {
    private static final String DEFAULT_WORKSPACE_NAME = "DEFAULT";
    private static final String DEFAULT_CURRENCY = "USD";

    private final UserAddService userAddService;
    private final IncomeAddService incomeAddService;
    private final WorkspaceAddService workspaceAddService;
    private final BankAddService bankAddService;
    private final WorkspaceCategoryService workspaceCategoryService;
    private final UserSettingService userSettingService;
    private final CurrencyAddService currencyAddService;
    private final WorkspaceRepository workspaceRepository;

    @Transactional(rollbackFor = Exception.class)
    public void finish(OnBoardingForm onBoardingForm) {
        var user = userAddService.createLogInUser(onBoardingForm.userType());
        var defaultWorkspace = this.createWorkspaces(onBoardingForm, user);
        this.addBanks(onBoardingForm, user);
        this.addDefaultCurrency(user);
        this.addCategories(onBoardingForm, defaultWorkspace);
        this.addInitialIncome(onBoardingForm, defaultWorkspace);
        userAddService.changeUserFirstLoginStatus(UserType.valueOf(onBoardingForm.userType()), user.getId());
    }

    private Workspace createWorkspaces(OnBoardingForm onBoardingForm, User user) {
        // 1. Siempre crear DEFAULT primero
        workspaceAddService.createWorkspace(new AddWorkspaceRecord(DEFAULT_WORKSPACE_NAME));

        // 2. Crear los workspaces adicionales (excluyendo DEFAULT si vino en la lista)
        onBoardingForm.accountsToAdd().stream()
                .filter(account -> !DEFAULT_WORKSPACE_NAME.equals(account))
                .forEach(account ->
                        workspaceAddService.createWorkspace(new AddWorkspaceRecord(account)));

        // 3. Setear DEFAULT como workspace por defecto y retornarlo
        var defaultWorkspace = workspaceRepository.findWorkspaceByNameAndOwnerId(
                DEFAULT_WORKSPACE_NAME, user.getId()).orElseThrow();
        userSettingService.upsertForUser(user, UserSettingKey.DEFAULT_WORKSPACE, defaultWorkspace.getId());
        return defaultWorkspace;
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

    private void addCategories(OnBoardingForm onBoardingForm, Workspace defaultWorkspace) {
        workspaceCategoryService.addCategories(defaultWorkspace, onBoardingForm.categoriesToAdd());
        workspaceCategoryService.addDefaultCategories(defaultWorkspace);
    }

    private void addInitialIncome(OnBoardingForm onBoardingForm, Workspace defaultWorkspace) {
        if (onBoardingForm.onBoardingAmount().bank() != null
                && onBoardingForm.onBoardingAmount().currency() != null
                && onBoardingForm.onBoardingAmount().amount() != null) {
            incomeAddService.loadIncome(new IncomeToAdd(onBoardingForm.onBoardingAmount().bank(),
                    new CurrencyRecord(onBoardingForm.onBoardingAmount().currency(), null),
                    onBoardingForm.onBoardingAmount().amount()),
                    defaultWorkspace);
        }
    }
}
