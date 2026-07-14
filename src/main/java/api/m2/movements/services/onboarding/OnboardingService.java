package api.m2.movements.services.onboarding;

import api.m2.movements.enums.UserSettingKey;
import api.m2.movements.records.currencies.CurrencyRecord;
import api.m2.movements.records.income.IncomeToAdd;
import api.m2.movements.records.onboarding.BankToAdd;
import api.m2.movements.records.onboarding.OnBoardingForm;
import api.m2.movements.records.workspaces.AddWorkspaceRecord;
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

    @Transactional(rollbackFor = Exception.class)
    public void finish(OnBoardingForm onBoardingForm) {
        var user = userAddService.createLogInUser(onBoardingForm.userType());
        var workspacesToAdd = onBoardingForm.accountsToAdd().stream()
                .map(AddWorkspaceRecord::new)
                .toList();

        var defaultWorkspace = workspaceAddService.createWorkspaces(user, workspacesToAdd)
                .stream().filter(workspaceAdded -> DEFAULT_WORKSPACE_NAME.equals(workspaceAdded.description()))
                .findFirst()
                .orElseThrow();

        userSettingService.upsertForUser(user.id(), UserSettingKey.DEFAULT_WORKSPACE, defaultWorkspace.id());

        this.addBanks(onBoardingForm, user.id());
        this.addDefaultCurrency(user.id());
        this.addCategories(onBoardingForm, defaultWorkspace.id());
        this.addInitialIncome(onBoardingForm, defaultWorkspace.id());
        userAddService.changeUserFirstLoginStatus(user.id());
    }

    private void addBanks(OnBoardingForm onBoardingForm, Long userId) {
        List<BankToAdd> banks = onBoardingForm.banksToAdd();
        if (banks.isEmpty()) {
            return;
        }

        var descriptions = banks.stream().map(BankToAdd::description).toList();
        var banksByDescription = bankAddService.addBanksToUser(descriptions, userId);

        var defaultBank = banks.stream()
                .filter(BankToAdd::isDefault)
                .findFirst()
                .orElse(banks.getFirst());

        userSettingService.upsertForUser(userId, UserSettingKey.DEFAULT_BANK,
                banksByDescription.get(defaultBank.description()).getId());
    }

    private void addDefaultCurrency(Long userId) {
        var usd = currencyAddService.findBySymbol(DEFAULT_CURRENCY);
        userSettingService.upsertForUser(userId, UserSettingKey.DEFAULT_CURRENCY, usd.getId());
    }

    private void addCategories(OnBoardingForm onBoardingForm, Long defaultWorkspaceId) {
        workspaceCategoryService.addCategories(defaultWorkspaceId, onBoardingForm.categoriesToAdd());
        workspaceCategoryService.addDefaultCategories(defaultWorkspaceId);
    }

    private void addInitialIncome(OnBoardingForm onBoardingForm, Long defaultWorkspaceId) {
        var amount = onBoardingForm.onBoardingAmount();
        if (amount != null
                && amount.bank() != null
                && amount.currency() != null
                && amount.amount() != null) {
            incomeAddService.loadIncome(new IncomeToAdd(amount.bank(),
                    new CurrencyRecord(amount.currency(), null),
                    amount.amount()),
                    defaultWorkspaceId);
        }
    }
}
