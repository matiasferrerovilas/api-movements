package api.m2.movements.unit.services

import api.m2.movements.entities.Bank
import api.m2.movements.entities.Currency
import api.m2.movements.entities.User
import api.m2.movements.entities.Workspace
import api.m2.movements.enums.UserSettingKey
import api.m2.movements.enums.UserType
import api.m2.movements.records.workspaces.AddWorkspaceRecord
import api.m2.movements.records.income.IncomeToAdd
import api.m2.movements.records.onboarding.BankToAdd
import api.m2.movements.records.onboarding.OnBoardingAmount
import api.m2.movements.records.onboarding.OnBoardingForm
import api.m2.movements.repositories.WorkspaceRepository
import api.m2.movements.services.banks.BankService
import api.m2.movements.services.category.UserCategoryService
import api.m2.movements.services.currencies.CurrencyAddService
import api.m2.movements.services.workspaces.WorkspaceAddService
import api.m2.movements.services.income.IncomeAddService
import api.m2.movements.services.onboarding.OnboardingService
import api.m2.movements.services.settings.UserSettingService
import api.m2.movements.services.user.UserAddService
import spock.lang.Specification

class OnboardingServiceTest extends Specification {

    UserAddService userAddService = Mock(UserAddService)
    IncomeAddService incomeAddService = Mock(IncomeAddService)
    WorkspaceAddService workspaceAddService = Mock(WorkspaceAddService)
    BankService bankService = Mock(BankService)
    UserCategoryService userCategoryService = Mock(UserCategoryService)
    UserSettingService userSettingService = Mock(UserSettingService)
    CurrencyAddService currencyAddService = Mock(CurrencyAddService)
    WorkspaceRepository workspaceRepository = Mock(WorkspaceRepository)

    OnboardingService service

    def setup() {
        service = new OnboardingService(userAddService, incomeAddService, workspaceAddService,
                bankService, userCategoryService, userSettingService, currencyAddService, workspaceRepository)
    }

    def "finish - should create user, accounts, banks, categories and income when all fields are present"() {
        given:
        def amount = new OnBoardingAmount(new BigDecimal("1500.00"), "DEFAULT", "GALICIA", "ARS")
        def banks = [new BankToAdd("GALICIA", true), new BankToAdd("SANTANDER", false)]
        def categories = ["COMIDA", "TRANSPORTE"]
        def form = new OnBoardingForm(amount, "CONSUMER", ["Viajes", "Casa"], categories, banks)
        def user = Stub(User) { getId() >> 42L }
        def galiciaBank = Stub(Bank) { getId() >> 10L }
        def santanderBank = Stub(Bank) { getId() >> 11L }
        def usd = Stub(Currency) { getId() >> 1L }
        def defaultWorkspace = Stub(Workspace) { getId() >> 100L }

        userAddService.createLogInUser() >> user
        bankService.addBankToUser("GALICIA", user) >> galiciaBank
        bankService.addBankToUser("SANTANDER", user) >> santanderBank
        currencyAddService.findBySymbol("USD") >> usd
        workspaceRepository.findWorkspaceByNameAndOwnerId("DEFAULT", 42L) >> Optional.of(defaultWorkspace)

        when:
        service.finish(form)

        then:
        1 * workspaceAddService.createWorkspace(new AddWorkspaceRecord("DEFAULT"))
        1 * workspaceAddService.createWorkspace(new AddWorkspaceRecord("Viajes"))
        1 * workspaceAddService.createWorkspace(new AddWorkspaceRecord("Casa"))
        1 * userSettingService.upsertForUser(user, UserSettingKey.DEFAULT_WORKSPACE, 100L)
        1 * bankService.addBankToUser("GALICIA", user) >> galiciaBank
        1 * userSettingService.upsertForUser(user, UserSettingKey.DEFAULT_BANK, 10L)
        1 * bankService.addBankToUser("SANTANDER", user) >> santanderBank
        0 * userSettingService.upsertForUser(user, UserSettingKey.DEFAULT_BANK, 11L)
        1 * userSettingService.upsertForUser(user, UserSettingKey.DEFAULT_CURRENCY, 1L)
        1 * userCategoryService.addCategories(user, categories)
        1 * userCategoryService.addDefaultCategories(user)
        1 * incomeAddService.loadIncome(_ as IncomeToAdd) >> { List args ->
            def income = args[0] as IncomeToAdd
            assert income.bank() == "GALICIA"
            assert income.currency().symbol() == "ARS"
            assert income.amount() == new BigDecimal("1500.00")
            assert income.workspace() == "DEFAULT"
        }
        1 * userAddService.changeUserFirstLoginStatus(UserType.CONSUMER, 42L)
    }

    def "finish - should set first bank as default when no bank has isDefault true"() {
        given:
        def amount = new OnBoardingAmount(null, null, null, null)
        def banks = [new BankToAdd("GALICIA", false), new BankToAdd("SANTANDER", false)]
        def form = new OnBoardingForm(amount, "CONSUMER", [], [], banks)
        def user = Stub(User) { getId() >> 1L }
        def galiciaBank = Stub(Bank) { getId() >> 10L }
        def santanderBank = Stub(Bank) { getId() >> 11L }
        def usd = Stub(Currency) { getId() >> 1L }
        def defaultWorkspace = Stub(Workspace) { getId() >> 100L }

        userAddService.createLogInUser() >> user
        bankService.addBankToUser("GALICIA", user) >> galiciaBank
        bankService.addBankToUser("SANTANDER", user) >> santanderBank
        currencyAddService.findBySymbol("USD") >> usd
        workspaceRepository.findWorkspaceByNameAndOwnerId("DEFAULT", 1L) >> Optional.of(defaultWorkspace)

        when:
        service.finish(form)

        then:
        1 * workspaceAddService.createWorkspace(new AddWorkspaceRecord("DEFAULT"))
        1 * userSettingService.upsertForUser(user, UserSettingKey.DEFAULT_WORKSPACE, 100L)
        1 * userSettingService.upsertForUser(user, UserSettingKey.DEFAULT_BANK, 10L)
        0 * userSettingService.upsertForUser(user, UserSettingKey.DEFAULT_BANK, 11L)
    }

    def "finish - should set only bank as default when there is exactly one bank"() {
        given:
        def amount = new OnBoardingAmount(null, null, null, null)
        def form = new OnBoardingForm(amount, "CONSUMER", [], [], [new BankToAdd("GALICIA", false)])
        def user = Stub(User) { getId() >> 1L }
        def galiciaBank = Stub(Bank) { getId() >> 10L }
        def usd = Stub(Currency) { getId() >> 1L }
        def defaultWorkspace = Stub(Workspace) { getId() >> 100L }

        userAddService.createLogInUser() >> user
        bankService.addBankToUser("GALICIA", user) >> galiciaBank
        currencyAddService.findBySymbol("USD") >> usd
        workspaceRepository.findWorkspaceByNameAndOwnerId("DEFAULT", 1L) >> Optional.of(defaultWorkspace)

        when:
        service.finish(form)

        then:
        1 * workspaceAddService.createWorkspace(new AddWorkspaceRecord("DEFAULT"))
        1 * userSettingService.upsertForUser(user, UserSettingKey.DEFAULT_BANK, 10L)
    }

    def "finish - should respect explicit isDefault when present"() {
        given:
        def amount = new OnBoardingAmount(null, null, null, null)
        def banks = [new BankToAdd("GALICIA", false), new BankToAdd("SANTANDER", true)]
        def form = new OnBoardingForm(amount, "CONSUMER", [], [], banks)
        def user = Stub(User) { getId() >> 1L }
        def galiciaBank = Stub(Bank) { getId() >> 10L }
        def santanderBank = Stub(Bank) { getId() >> 11L }
        def usd = Stub(Currency) { getId() >> 1L }
        def defaultWorkspace = Stub(Workspace) { getId() >> 100L }

        userAddService.createLogInUser() >> user
        bankService.addBankToUser("GALICIA", user) >> galiciaBank
        bankService.addBankToUser("SANTANDER", user) >> santanderBank
        currencyAddService.findBySymbol("USD") >> usd
        workspaceRepository.findWorkspaceByNameAndOwnerId("DEFAULT", 1L) >> Optional.of(defaultWorkspace)

        when:
        service.finish(form)

        then:
        1 * workspaceAddService.createWorkspace(new AddWorkspaceRecord("DEFAULT"))
        0 * userSettingService.upsertForUser(user, UserSettingKey.DEFAULT_BANK, 10L)
        1 * userSettingService.upsertForUser(user, UserSettingKey.DEFAULT_BANK, 11L)
    }

    def "finish - should set DEFAULT_CURRENCY to USD"() {
        given:
        def amount = new OnBoardingAmount(null, null, null, null)
        def form = new OnBoardingForm(amount, "CONSUMER", [], [], [])
        def user = Stub(User) { getId() >> 1L }
        def usd = Stub(Currency) { getId() >> 5L }
        def defaultWorkspace = Stub(Workspace) { getId() >> 100L }

        userAddService.createLogInUser() >> user
        currencyAddService.findBySymbol("USD") >> usd
        workspaceRepository.findWorkspaceByNameAndOwnerId("DEFAULT", 1L) >> Optional.of(defaultWorkspace)

        when:
        service.finish(form)

        then:
        1 * workspaceAddService.createWorkspace(new AddWorkspaceRecord("DEFAULT"))
        1 * currencyAddService.findBySymbol("USD") >> usd
        1 * userSettingService.upsertForUser(user, UserSettingKey.DEFAULT_CURRENCY, 5L)
    }

    def "finish - should always call addDefaultCategories regardless of categoriesToAdd"() {
        given:
        def amount = new OnBoardingAmount(null, null, null, null)
        def form = new OnBoardingForm(amount, "CONSUMER", [], ["HOGAR"], [])
        def user = Stub(User) { getId() >> 1L }
        def usd = Stub(Currency) { getId() >> 1L }
        def defaultWorkspace = Stub(Workspace) { getId() >> 100L }

        userAddService.createLogInUser() >> user
        currencyAddService.findBySymbol("USD") >> usd
        workspaceRepository.findWorkspaceByNameAndOwnerId("DEFAULT", 1L) >> Optional.of(defaultWorkspace)

        when:
        service.finish(form)

        then:
        1 * workspaceAddService.createWorkspace(new AddWorkspaceRecord("DEFAULT"))
        1 * userCategoryService.addCategories(user, ["HOGAR"])
        1 * userCategoryService.addDefaultCategories(user)
    }

    def "finish - should call addDefaultCategories even when categoriesToAdd is empty"() {
        given:
        def amount = new OnBoardingAmount(null, null, null, null)
        def form = new OnBoardingForm(amount, "CONSUMER", [], [], [])
        def user = Stub(User) { getId() >> 1L }
        def usd = Stub(Currency) { getId() >> 1L }
        def defaultWorkspace = Stub(Workspace) { getId() >> 100L }

        userAddService.createLogInUser() >> user
        currencyAddService.findBySymbol("USD") >> usd
        workspaceRepository.findWorkspaceByNameAndOwnerId("DEFAULT", 1L) >> Optional.of(defaultWorkspace)

        when:
        service.finish(form)

        then:
        1 * workspaceAddService.createWorkspace(new AddWorkspaceRecord("DEFAULT"))
        1 * userCategoryService.addCategories(user, [])
        1 * userCategoryService.addDefaultCategories(user)
    }

    def "finish - should skip income when bank is null"() {
        given:
        def amount = new OnBoardingAmount(new BigDecimal("1000.00"), "DEFAULT", null, "ARS")
        def form = new OnBoardingForm(amount, "CONSUMER", ["Hogar"], [], [])
        def user = Stub(User) { getId() >> 1L }
        def usd = Stub(Currency) { getId() >> 1L }
        def defaultWorkspace = Stub(Workspace) { getId() >> 100L }

        userAddService.createLogInUser() >> user
        currencyAddService.findBySymbol("USD") >> usd
        workspaceRepository.findWorkspaceByNameAndOwnerId("DEFAULT", 1L) >> Optional.of(defaultWorkspace)

        when:
        service.finish(form)

        then:
        1 * workspaceAddService.createWorkspace(new AddWorkspaceRecord("DEFAULT"))
        1 * workspaceAddService.createWorkspace(new AddWorkspaceRecord("Hogar"))
        0 * incomeAddService.loadIncome(_ as IncomeToAdd)
        1 * userAddService.changeUserFirstLoginStatus(UserType.CONSUMER, 1L)
    }

    def "finish - should skip income when currency is null"() {
        given:
        def amount = new OnBoardingAmount(new BigDecimal("1000.00"), "DEFAULT", "GALICIA", null)
        def form = new OnBoardingForm(amount, "COMPANY", ["Gastos"], [], [])
        def user = Stub(User) { getId() >> 2L }
        def usd = Stub(Currency) { getId() >> 1L }
        def defaultWorkspace = Stub(Workspace) { getId() >> 100L }

        userAddService.createLogInUser() >> user
        currencyAddService.findBySymbol("USD") >> usd
        workspaceRepository.findWorkspaceByNameAndOwnerId("DEFAULT", 2L) >> Optional.of(defaultWorkspace)

        when:
        service.finish(form)

        then:
        1 * workspaceAddService.createWorkspace(new AddWorkspaceRecord("DEFAULT"))
        1 * workspaceAddService.createWorkspace(new AddWorkspaceRecord("Gastos"))
        0 * incomeAddService.loadIncome(_ as IncomeToAdd)
        1 * userAddService.changeUserFirstLoginStatus(UserType.COMPANY, 2L)
    }

    def "finish - should skip income when amount is null"() {
        given:
        def amount = new OnBoardingAmount(null, "DEFAULT", "GALICIA", "ARS")
        def form = new OnBoardingForm(amount, "CONSUMER", ["Personal"], [], [])
        def user = Stub(User) { getId() >> 3L }
        def usd = Stub(Currency) { getId() >> 1L }
        def defaultWorkspace = Stub(Workspace) { getId() >> 100L }

        userAddService.createLogInUser() >> user
        currencyAddService.findBySymbol("USD") >> usd
        workspaceRepository.findWorkspaceByNameAndOwnerId("DEFAULT", 3L) >> Optional.of(defaultWorkspace)

        when:
        service.finish(form)

        then:
        1 * workspaceAddService.createWorkspace(new AddWorkspaceRecord("DEFAULT"))
        1 * workspaceAddService.createWorkspace(new AddWorkspaceRecord("Personal"))
        0 * incomeAddService.loadIncome(_ as IncomeToAdd)
        1 * userAddService.changeUserFirstLoginStatus(UserType.CONSUMER, 3L)
    }

    def "finish - should create DEFAULT and additional accounts from accountsToAdd"() {
        given:
        def amount = new OnBoardingAmount(null, null, null, null)
        def form = new OnBoardingForm(amount, "CONSUMER", ["Alpha", "Beta", "Gamma"], [], [])
        def user = Stub(User) { getId() >> 10L }
        def usd = Stub(Currency) { getId() >> 1L }
        def defaultWorkspace = Stub(Workspace) { getId() >> 100L }

        userAddService.createLogInUser() >> user
        currencyAddService.findBySymbol("USD") >> usd
        workspaceRepository.findWorkspaceByNameAndOwnerId("DEFAULT", 10L) >> Optional.of(defaultWorkspace)

        when:
        service.finish(form)

        then:
        1 * workspaceAddService.createWorkspace(new AddWorkspaceRecord("DEFAULT"))
        1 * workspaceAddService.createWorkspace(new AddWorkspaceRecord("Alpha"))
        1 * workspaceAddService.createWorkspace(new AddWorkspaceRecord("Beta"))
        1 * workspaceAddService.createWorkspace(new AddWorkspaceRecord("Gamma"))
        1 * userSettingService.upsertForUser(user, UserSettingKey.DEFAULT_WORKSPACE, 100L)
    }

    def "finish - should not call addBankToUser or set DEFAULT_BANK when banksToAdd is empty"() {
        given:
        def amount = new OnBoardingAmount(null, null, null, null)
        def form = new OnBoardingForm(amount, "CONSUMER", [], [], [])
        def user = Stub(User) { getId() >> 5L }
        def usd = Stub(Currency) { getId() >> 1L }
        def defaultWorkspace = Stub(Workspace) { getId() >> 100L }

        userAddService.createLogInUser() >> user
        currencyAddService.findBySymbol("USD") >> usd
        workspaceRepository.findWorkspaceByNameAndOwnerId("DEFAULT", 5L) >> Optional.of(defaultWorkspace)

        when:
        service.finish(form)

        then:
        1 * workspaceAddService.createWorkspace(new AddWorkspaceRecord("DEFAULT"))
        0 * bankService.addBankToUser(_ as String, _ as User)
        0 * userSettingService.upsertForUser(_ as User, UserSettingKey.DEFAULT_BANK, _ as Long)
    }

    def "finish - should not duplicate DEFAULT when it is in accountsToAdd"() {
        given:
        def amount = new OnBoardingAmount(null, null, null, null)
        def form = new OnBoardingForm(amount, "CONSUMER", ["DEFAULT", "Other"], [], [])
        def user = Stub(User) { getId() >> 1L }
        def usd = Stub(Currency) { getId() >> 1L }
        def workspace = Stub(Workspace) { getId() >> 100L }

        userAddService.createLogInUser() >> user
        currencyAddService.findBySymbol("USD") >> usd
        workspaceRepository.findWorkspaceByNameAndOwnerId("DEFAULT", 1L) >> Optional.of(workspace)

        when:
        service.finish(form)

        then:
        1 * workspaceAddService.createWorkspace(new AddWorkspaceRecord("DEFAULT"))
        1 * workspaceAddService.createWorkspace(new AddWorkspaceRecord("Other"))
        0 * workspaceAddService.createWorkspace(new AddWorkspaceRecord("DEFAULT")) // no se duplica
        1 * userSettingService.upsertForUser(user, UserSettingKey.DEFAULT_WORKSPACE, 100L)
    }

    def "finish - should always create DEFAULT even when accountsToAdd is empty"() {
        given:
        def amount = new OnBoardingAmount(null, null, null, null)
        def form = new OnBoardingForm(amount, "CONSUMER", [], [], [])
        def user = Stub(User) { getId() >> 1L }
        def usd = Stub(Currency) { getId() >> 1L }
        def workspace = Stub(Workspace) { getId() >> 100L }

        userAddService.createLogInUser() >> user
        currencyAddService.findBySymbol("USD") >> usd
        workspaceRepository.findWorkspaceByNameAndOwnerId("DEFAULT", 1L) >> Optional.of(workspace)

        when:
        service.finish(form)

        then:
        1 * workspaceAddService.createWorkspace(new AddWorkspaceRecord("DEFAULT"))
        1 * userSettingService.upsertForUser(user, UserSettingKey.DEFAULT_WORKSPACE, 100L)
    }
}
