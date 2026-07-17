package api.m2.movements.unit.services

import api.m2.movements.entities.commons.Bank
import api.m2.movements.entities.commons.Currency

import api.m2.movements.clients.identity.requests.UserToAdd
import api.m2.movements.enums.UserSettingKey
import api.m2.movements.clients.identity.response.WorkspaceAdded
import api.m2.movements.records.income.IncomeToAdd
import api.m2.movements.records.onboarding.BankToAdd
import api.m2.movements.records.onboarding.OnBoardingAmount
import api.m2.movements.records.onboarding.OnBoardingForm
import api.m2.movements.services.banks.BankAddService
import api.m2.movements.services.category.WorkspaceCategoryService
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
    BankAddService bankAddService = Mock(BankAddService)
    WorkspaceCategoryService workspaceCategoryService = Mock(WorkspaceCategoryService)
    UserSettingService userSettingService = Mock(UserSettingService)
    CurrencyAddService currencyAddService = Mock(CurrencyAddService)

    OnboardingService service

    def setup() {
        service = new OnboardingService(userAddService, incomeAddService, workspaceAddService,
                bankAddService, workspaceCategoryService, userSettingService, currencyAddService)
    }

    def user(Long id) {
        return UserToAdd.builder().id(id).email("test@test.com").build()
    }

    def "finish - should create user, accounts, banks, categories and income when all fields are present"() {
        given:
        def amount = new OnBoardingAmount(new BigDecimal("1500.00"), "DEFAULT", "GALICIA", "ARS")
        def banks = [new BankToAdd("GALICIA", true), new BankToAdd("SANTANDER", false)]
        def categories = ["COMIDA", "TRANSPORTE"]
        def form = new OnBoardingForm(amount, "PERSONAL", ["Viajes", "Casa"], categories, banks)
        def loggedUser = user(42L)
        def galiciaBank = Stub(Bank) { getId() >> 10L }
        def santanderBank = Stub(Bank) { getId() >> 11L }
        def usd = Stub(Currency) { getId() >> 1L }

        userAddService.createLogInUser("PERSONAL") >> loggedUser
        workspaceAddService.createWorkspaces(loggedUser, _ as List) >> [
                new WorkspaceAdded(100L, "DEFAULT"),
                new WorkspaceAdded(101L, "Viajes"),
                new WorkspaceAdded(102L, "Casa"),
        ]
        bankAddService.addBanksToUser(["GALICIA", "SANTANDER"], 42L) >> [GALICIA: galiciaBank, SANTANDER: santanderBank]
        currencyAddService.findBySymbol("USD") >> usd

        when:
        service.finish(form)

        then:
        1 * userSettingService.upsertForUser(42L, UserSettingKey.DEFAULT_WORKSPACE, 100L)
        1 * userSettingService.upsertForUser(42L, UserSettingKey.DEFAULT_BANK, 10L)
        1 * userSettingService.upsertForUser(42L, UserSettingKey.DEFAULT_CURRENCY, 1L)
        1 * workspaceCategoryService.addCategories(100L, categories)
        1 * workspaceCategoryService.addDefaultCategories(100L)
        1 * incomeAddService.loadIncome(_ as IncomeToAdd, 100L) >> { List args ->
            def income = args[0] as IncomeToAdd
            assert income.bank() == "GALICIA"
            assert income.currency().symbol() == "ARS"
            assert income.amount() == new BigDecimal("1500.00")
        }
        1 * userAddService.changeUserFirstLoginStatus(42L)
    }

    def "finish - should set first bank as default when no bank has isDefault true"() {
        given:
        def amount = new OnBoardingAmount(null, null, null, null)
        def banks = [new BankToAdd("GALICIA", false), new BankToAdd("SANTANDER", false)]
        def form = new OnBoardingForm(amount, "PERSONAL", [], [], banks)
        def loggedUser = user(1L)
        def galiciaBank = Stub(Bank) { getId() >> 10L }
        def santanderBank = Stub(Bank) { getId() >> 11L }
        def usd = Stub(Currency) { getId() >> 1L }

        userAddService.createLogInUser("PERSONAL") >> loggedUser
        workspaceAddService.createWorkspaces(loggedUser, _ as List) >> [new WorkspaceAdded(100L, "DEFAULT")]
        bankAddService.addBanksToUser(["GALICIA", "SANTANDER"], 1L) >> [GALICIA: galiciaBank, SANTANDER: santanderBank]
        currencyAddService.findBySymbol("USD") >> usd

        when:
        service.finish(form)

        then:
        1 * userSettingService.upsertForUser(1L, UserSettingKey.DEFAULT_WORKSPACE, 100L)
        1 * userSettingService.upsertForUser(1L, UserSettingKey.DEFAULT_BANK, 10L)
        0 * userSettingService.upsertForUser(1L, UserSettingKey.DEFAULT_BANK, 11L)
    }

    def "finish - should set only bank as default when there is exactly one bank"() {
        given:
        def amount = new OnBoardingAmount(null, null, null, null)
        def form = new OnBoardingForm(amount, "PERSONAL", [], [], [new BankToAdd("GALICIA", false)])
        def loggedUser = user(1L)
        def galiciaBank = Stub(Bank) { getId() >> 10L }
        def usd = Stub(Currency) { getId() >> 1L }

        userAddService.createLogInUser("PERSONAL") >> loggedUser
        workspaceAddService.createWorkspaces(loggedUser, _ as List) >> [new WorkspaceAdded(100L, "DEFAULT")]
        bankAddService.addBanksToUser(["GALICIA"], 1L) >> [GALICIA: galiciaBank]
        currencyAddService.findBySymbol("USD") >> usd

        when:
        service.finish(form)

        then:
        1 * userSettingService.upsertForUser(1L, UserSettingKey.DEFAULT_BANK, 10L)
    }

    def "finish - should respect explicit isDefault when present"() {
        given:
        def amount = new OnBoardingAmount(null, null, null, null)
        def banks = [new BankToAdd("GALICIA", false), new BankToAdd("SANTANDER", true)]
        def form = new OnBoardingForm(amount, "PERSONAL", [], [], banks)
        def loggedUser = user(1L)
        def galiciaBank = Stub(Bank) { getId() >> 10L }
        def santanderBank = Stub(Bank) { getId() >> 11L }
        def usd = Stub(Currency) { getId() >> 1L }

        userAddService.createLogInUser("PERSONAL") >> loggedUser
        workspaceAddService.createWorkspaces(loggedUser, _ as List) >> [new WorkspaceAdded(100L, "DEFAULT")]
        bankAddService.addBanksToUser(["GALICIA", "SANTANDER"], 1L) >> [GALICIA: galiciaBank, SANTANDER: santanderBank]
        currencyAddService.findBySymbol("USD") >> usd

        when:
        service.finish(form)

        then:
        0 * userSettingService.upsertForUser(1L, UserSettingKey.DEFAULT_BANK, 10L)
        1 * userSettingService.upsertForUser(1L, UserSettingKey.DEFAULT_BANK, 11L)
    }

    def "finish - should set DEFAULT_CURRENCY to USD"() {
        given:
        def amount = new OnBoardingAmount(null, null, null, null)
        def form = new OnBoardingForm(amount, "PERSONAL", [], [], [])
        def loggedUser = user(1L)
        def usd = Stub(Currency) { getId() >> 5L }

        userAddService.createLogInUser("PERSONAL") >> loggedUser
        workspaceAddService.createWorkspaces(loggedUser, _ as List) >> [new WorkspaceAdded(100L, "DEFAULT")]
        currencyAddService.findBySymbol("USD") >> usd

        when:
        service.finish(form)

        then:
        1 * currencyAddService.findBySymbol("USD") >> usd
        1 * userSettingService.upsertForUser(1L, UserSettingKey.DEFAULT_CURRENCY, 5L)
    }

    def "finish - should always call addDefaultCategories regardless of categoriesToAdd"() {
        given:
        def amount = new OnBoardingAmount(null, null, null, null)
        def form = new OnBoardingForm(amount, "PERSONAL", [], ["HOGAR"], [])
        def loggedUser = user(1L)
        def usd = Stub(Currency) { getId() >> 1L }

        userAddService.createLogInUser("PERSONAL") >> loggedUser
        workspaceAddService.createWorkspaces(loggedUser, _ as List) >> [new WorkspaceAdded(100L, "DEFAULT")]
        currencyAddService.findBySymbol("USD") >> usd

        when:
        service.finish(form)

        then:
        1 * workspaceCategoryService.addCategories(100L, ["HOGAR"])
        1 * workspaceCategoryService.addDefaultCategories(100L)
    }

    def "finish - should call addDefaultCategories even when categoriesToAdd is empty"() {
        given:
        def amount = new OnBoardingAmount(null, null, null, null)
        def form = new OnBoardingForm(amount, "PERSONAL", [], [], [])
        def loggedUser = user(1L)
        def usd = Stub(Currency) { getId() >> 1L }

        userAddService.createLogInUser("PERSONAL") >> loggedUser
        workspaceAddService.createWorkspaces(loggedUser, _ as List) >> [new WorkspaceAdded(100L, "DEFAULT")]
        currencyAddService.findBySymbol("USD") >> usd

        when:
        service.finish(form)

        then:
        1 * workspaceCategoryService.addCategories(100L, [])
        1 * workspaceCategoryService.addDefaultCategories(100L)
    }

    def "finish - should skip income when bank is null"() {
        given:
        def amount = new OnBoardingAmount(new BigDecimal("1000.00"), "DEFAULT", null, "ARS")
        def form = new OnBoardingForm(amount, "PERSONAL", ["Hogar"], [], [])
        def loggedUser = user(1L)
        def usd = Stub(Currency) { getId() >> 1L }

        userAddService.createLogInUser("PERSONAL") >> loggedUser
        workspaceAddService.createWorkspaces(loggedUser, _ as List) >> [
                new WorkspaceAdded(100L, "DEFAULT"), new WorkspaceAdded(101L, "Hogar")]
        currencyAddService.findBySymbol("USD") >> usd

        when:
        service.finish(form)

        then:
        0 * incomeAddService.loadIncome(_ as IncomeToAdd, _ as Long)
        1 * userAddService.changeUserFirstLoginStatus(1L)
    }

    def "finish - should skip income when currency is null"() {
        given:
        def amount = new OnBoardingAmount(new BigDecimal("1000.00"), "DEFAULT", "GALICIA", null)
        def form = new OnBoardingForm(amount, "ENTERPRISE", ["Gastos"], [], [])
        def loggedUser = user(2L)
        def usd = Stub(Currency) { getId() >> 1L }

        userAddService.createLogInUser("ENTERPRISE") >> loggedUser
        workspaceAddService.createWorkspaces(loggedUser, _ as List) >> [
                new WorkspaceAdded(100L, "DEFAULT"), new WorkspaceAdded(101L, "Gastos")]
        currencyAddService.findBySymbol("USD") >> usd

        when:
        service.finish(form)

        then:
        0 * incomeAddService.loadIncome(_ as IncomeToAdd, _ as Long)
        1 * userAddService.changeUserFirstLoginStatus(2L)
    }

    def "finish - should skip income when amount is null"() {
        given:
        def amount = new OnBoardingAmount(null, "DEFAULT", "GALICIA", "ARS")
        def form = new OnBoardingForm(amount, "PERSONAL", ["Personal"], [], [])
        def loggedUser = user(3L)
        def usd = Stub(Currency) { getId() >> 1L }

        userAddService.createLogInUser("PERSONAL") >> loggedUser
        workspaceAddService.createWorkspaces(loggedUser, _ as List) >> [
                new WorkspaceAdded(100L, "DEFAULT"), new WorkspaceAdded(101L, "Personal")]
        currencyAddService.findBySymbol("USD") >> usd

        when:
        service.finish(form)

        then:
        0 * incomeAddService.loadIncome(_ as IncomeToAdd, _ as Long)
        1 * userAddService.changeUserFirstLoginStatus(3L)
    }

    def "finish - should not call addBankToUser or set DEFAULT_BANK when banksToAdd is empty"() {
        given:
        def amount = new OnBoardingAmount(null, null, null, null)
        def form = new OnBoardingForm(amount, "PERSONAL", [], [], [])
        def loggedUser = user(5L)
        def usd = Stub(Currency) { getId() >> 1L }

        userAddService.createLogInUser("PERSONAL") >> loggedUser
        workspaceAddService.createWorkspaces(loggedUser, _ as List) >> [new WorkspaceAdded(100L, "DEFAULT")]
        currencyAddService.findBySymbol("USD") >> usd

        when:
        service.finish(form)

        then:
        0 * bankAddService.addBanksToUser(_ as List, _ as Long)
        0 * userSettingService.upsertForUser(_ as Long, UserSettingKey.DEFAULT_BANK, _ as Long)
    }
}
