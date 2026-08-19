package api.m2.movements.services.onboarding;

import api.m2.movements.clients.identity.IdentityClient;
import api.m2.movements.enums.UserSettingKey;
import api.m2.movements.records.currencies.CurrencyRecord;
import api.m2.movements.records.currencies.CurrencyToAdd;
import api.m2.movements.records.income.IncomeToAdd;
import api.m2.movements.records.onboarding.BankToAdd;
import api.m2.movements.records.onboarding.OnBoardingForm;
import api.m2.movements.records.onboarding.WorkspaceToAdd;
import api.m2.movements.clients.identity.requests.AddWorkspaceRecord;
import api.m2.movements.services.banks.BankAddService;
import api.m2.movements.services.category.WorkspaceCategoryService;
import api.m2.movements.services.currencies.CurrencyAddService;
import api.m2.movements.services.currencies.WorkspaceCurrencyService;
import api.m2.movements.services.income.IncomeAddService;
import api.m2.movements.services.settings.UserSettingService;
import api.m2.movements.services.user.UserAddService;
import api.m2.movements.services.workspaces.WorkspaceAddService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
    private final WorkspaceCurrencyService workspaceCurrencyService;
    private final IdentityClient identityClient;

    @Transactional(rollbackFor = Exception.class)
    public void finish(OnBoardingForm onBoardingForm) {
        var user = userAddService.createLogInUser(onBoardingForm.userType());
        var workspacesToAdd = this.buildWorkspacesToAdd(onBoardingForm.workspacesToAdd());
        var defaultWorkspaceName = this.resolveDefaultWorkspaceName(onBoardingForm.workspacesToAdd());

        var defaultWorkspace = workspaceAddService.createWorkspaces(workspacesToAdd)
                .stream().filter(workspaceAdded -> defaultWorkspaceName.equals(workspaceAdded.description()))
                .findFirst()
                .orElseThrow();

        userSettingService.upsertForUser(user.id(), UserSettingKey.DEFAULT_WORKSPACE, defaultWorkspace.id());

        this.addBanks(onBoardingForm, user.id());
        // Las monedas del workspace se crean antes de fijar la default: esta última necesita
        // que ya exista la asociación workspace-moneda para poder guardar su id.
        workspaceCurrencyService.addDefaultCurrencies(defaultWorkspace.id());
        this.addCurrencies(onBoardingForm, defaultWorkspace.id());
        this.addDefaultCurrency(user.id(), defaultWorkspace.id());
        this.addCategories(onBoardingForm, defaultWorkspace.id());
        this.addInitialIncome(onBoardingForm, defaultWorkspace.id());
        userAddService.changeUserFirstLoginStatus(user.id());
    }

    private List<AddWorkspaceRecord> buildWorkspacesToAdd(List<WorkspaceToAdd> workspacesToAdd) {
        var addWorkspaceRecords = workspacesToAdd.stream()
                .map(WorkspaceToAdd::name)
                .map(AddWorkspaceRecord::new)
                .collect(Collectors.toList());

        boolean hasDefault = workspacesToAdd.stream().anyMatch(WorkspaceToAdd::isDefault);
        if (!hasDefault) {
            addWorkspaceRecords.add(new AddWorkspaceRecord(DEFAULT_WORKSPACE_NAME));
        }

        return addWorkspaceRecords;
    }

    private String resolveDefaultWorkspaceName(List<WorkspaceToAdd> workspacesToAdd) {
        return workspacesToAdd.stream()
                .filter(WorkspaceToAdd::isDefault)
                .map(WorkspaceToAdd::name)
                .findFirst()
                .orElse(DEFAULT_WORKSPACE_NAME);
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

    private void addCurrencies(OnBoardingForm onBoardingForm, Long workspaceId) {
        List<CurrencyToAdd> currencies = onBoardingForm.currenciesToAdd();
        if (currencies.isEmpty()) {
            return;
        }

        currencies.forEach(currencyToAdd -> {
            var currency = currencyAddService.addCurrency(currencyToAdd.symbol(), currencyToAdd.description());
            workspaceCurrencyService.ensureCurrencyInWorkspace(workspaceId, currency);
        });
    }

    /**
     * Guarda el id de la WorkspaceCurrency, no el de la Currency del catálogo global.
     * GET /workspace/currencies expone WorkspaceCurrencyRecord.id, así que ese es el id contra
     * el que las pantallas de configuración (web y mobile) comparan y escriben DEFAULT_CURRENCY.
     * Guardar el id del catálogo dejaba la moneda por defecto sin coincidir con ninguna opción
     * de la lista hasta que el usuario la elegía a mano.
     */
    private void addDefaultCurrency(Long userId, Long defaultWorkspaceId) {
        var usd = currencyAddService.findBySymbol(DEFAULT_CURRENCY);
        var workspaceCurrency = workspaceCurrencyService.ensureCurrencyInWorkspace(defaultWorkspaceId, usd);
        userSettingService.upsertForUser(userId, UserSettingKey.DEFAULT_CURRENCY, workspaceCurrency.getId());
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

    public void markTourAsSeen() {
        identityClient.markTourAsSeen();
    }
}
