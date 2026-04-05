package api.m2.movements.unit.services

import api.m2.movements.entities.Bank
import api.m2.movements.entities.User
import api.m2.movements.enums.UserSettingKey
import api.m2.movements.enums.UserType
import api.m2.movements.records.groups.AddGroupRecord
import api.m2.movements.records.income.IncomeToAdd
import api.m2.movements.records.onboarding.BankToAdd
import api.m2.movements.records.onboarding.OnBoardingAmount
import api.m2.movements.records.onboarding.OnBoardingForm
import api.m2.movements.services.banks.BankService
import api.m2.movements.services.category.UserCategoryService
import api.m2.movements.services.groups.GroupAddService
import api.m2.movements.services.income.IncomeAddService
import api.m2.movements.services.onboarding.OnboardingService
import api.m2.movements.services.settings.UserSettingService
import api.m2.movements.services.user.UserAddService
import spock.lang.Specification

class OnboardingServiceTest extends Specification {

    UserAddService userAddService = Mock(UserAddService)
    IncomeAddService incomeAddService = Mock(IncomeAddService)
    GroupAddService groupAddService = Mock(GroupAddService)
    BankService bankService = Mock(BankService)
    UserCategoryService userCategoryService = Mock(UserCategoryService)
    UserSettingService userSettingService = Mock(UserSettingService)

    OnboardingService service

    def setup() {
        service = new OnboardingService(userAddService, incomeAddService, groupAddService,
                bankService, userCategoryService, userSettingService)
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

        userAddService.createLogInUser() >> user
        bankService.addBankToUser("GALICIA", user) >> galiciaBank
        bankService.addBankToUser("SANTANDER", user) >> santanderBank

        when:
        service.finish(form)

        then:
        1 * groupAddService.createAccount(new AddGroupRecord("Viajes"))
        1 * groupAddService.createAccount(new AddGroupRecord("Casa"))
        1 * bankService.addBankToUser("GALICIA", user) >> galiciaBank
        1 * userSettingService.upsertForUser(user, UserSettingKey.DEFAULT_BANK, 10L)
        1 * bankService.addBankToUser("SANTANDER", user) >> santanderBank
        0 * userSettingService.upsertForUser(user, UserSettingKey.DEFAULT_BANK, 11L)
        1 * userCategoryService.addCategories(user, categories)
        1 * incomeAddService.loadIncome(_ as IncomeToAdd) >> { List args ->
            def income = args[0] as IncomeToAdd
            assert income.bank() == "GALICIA"
            assert income.currency() == "ARS"
            assert income.amount() == new BigDecimal("1500.00")
            assert income.group() == "DEFAULT"
        }
        1 * userAddService.changeUserFirstLoginStatus(UserType.CONSUMER, 42L)
    }

    def "finish - should skip income when bank is null"() {
        given:
        def amount = new OnBoardingAmount(new BigDecimal("1000.00"), "DEFAULT", null, "ARS")
        def form = new OnBoardingForm(amount, "CONSUMER", ["Hogar"], [], [])
        def user = Stub(User) { getId() >> 1L }

        userAddService.createLogInUser() >> user

        when:
        service.finish(form)

        then:
        0 * incomeAddService.loadIncome(_ as IncomeToAdd)
        1 * userAddService.changeUserFirstLoginStatus(UserType.CONSUMER, 1L)
    }

    def "finish - should skip income when currency is null"() {
        given:
        def amount = new OnBoardingAmount(new BigDecimal("1000.00"), "DEFAULT", "GALICIA", null)
        def form = new OnBoardingForm(amount, "COMPANY", ["Gastos"], [], [])
        def user = Stub(User) { getId() >> 2L }

        userAddService.createLogInUser() >> user

        when:
        service.finish(form)

        then:
        0 * incomeAddService.loadIncome(_ as IncomeToAdd)
        1 * userAddService.changeUserFirstLoginStatus(UserType.COMPANY, 2L)
    }

    def "finish - should skip income when amount is null"() {
        given:
        def amount = new OnBoardingAmount(null, "DEFAULT", "GALICIA", "ARS")
        def form = new OnBoardingForm(amount, "CONSUMER", ["Personal"], [], [])
        def user = Stub(User) { getId() >> 3L }

        userAddService.createLogInUser() >> user

        when:
        service.finish(form)

        then:
        0 * incomeAddService.loadIncome(_ as IncomeToAdd)
        1 * userAddService.changeUserFirstLoginStatus(UserType.CONSUMER, 3L)
    }

    def "finish - should create accounts for each entry in accountsToAdd"() {
        given:
        def amount = new OnBoardingAmount(null, null, null, null)
        def form = new OnBoardingForm(amount, "CONSUMER", ["Alpha", "Beta", "Gamma"], [], [])
        def user = Stub(User) { getId() >> 10L }

        userAddService.createLogInUser() >> user

        when:
        service.finish(form)

        then:
        1 * groupAddService.createAccount(new AddGroupRecord("Alpha"))
        1 * groupAddService.createAccount(new AddGroupRecord("Beta"))
        1 * groupAddService.createAccount(new AddGroupRecord("Gamma"))
    }

    def "finish - should not call addBankToUser or set DEFAULT_BANK when banksToAdd is empty"() {
        given:
        def amount = new OnBoardingAmount(null, null, null, null)
        def form = new OnBoardingForm(amount, "CONSUMER", [], [], [])
        def user = Stub(User) { getId() >> 5L }

        userAddService.createLogInUser() >> user

        when:
        service.finish(form)

        then:
        0 * bankService.addBankToUser(_ as String, _ as User)
        0 * userSettingService.upsertForUser(_ as User, UserSettingKey.DEFAULT_BANK, _ as Long)
    }

    def "finish - should not call addCategories when categoriesToAdd is empty"() {
        given:
        def amount = new OnBoardingAmount(null, null, null, null)
        def form = new OnBoardingForm(amount, "CONSUMER", [], [], [])
        def user = Stub(User) { getId() >> 6L }

        userAddService.createLogInUser() >> user

        when:
        service.finish(form)

        then:
        1 * userCategoryService.addCategories(user, [])
    }

    def "finish - should not set DEFAULT_BANK when no bank in banksToAdd has isDefault true"() {
        given:
        def amount = new OnBoardingAmount(null, null, null, null)
        def banks = [new BankToAdd("GALICIA", false), new BankToAdd("SANTANDER", false)]
        def form = new OnBoardingForm(amount, "CONSUMER", [], [], banks)
        def user = Stub(User) { getId() >> 7L }
        def galiciaBank = Stub(Bank) { getId() >> 10L }
        def santanderBank = Stub(Bank) { getId() >> 11L }

        userAddService.createLogInUser() >> user
        bankService.addBankToUser("GALICIA", user) >> galiciaBank
        bankService.addBankToUser("SANTANDER", user) >> santanderBank

        when:
        service.finish(form)

        then:
        1 * bankService.addBankToUser("GALICIA", user)
        1 * bankService.addBankToUser("SANTANDER", user)
        0 * userSettingService.upsertForUser(_ as User, UserSettingKey.DEFAULT_BANK, _ as Long)
    }
}
