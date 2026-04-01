package api.m2.movements.unit.services

import api.m2.movements.entities.User
import api.m2.movements.enums.UserType
import api.m2.movements.records.groups.AddGroupRecord
import api.m2.movements.records.income.IncomeToAdd
import api.m2.movements.records.onboarding.OnBoardingAmount
import api.m2.movements.records.onboarding.OnBoardingForm
import api.m2.movements.services.groups.GroupAddService
import api.m2.movements.services.income.IncomeAddService
import api.m2.movements.services.onboarding.OnboardingService
import api.m2.movements.services.user.UserAddService
import spock.lang.Specification

class OnboardingServiceTest extends Specification {

    UserAddService userAddService = Mock(UserAddService)
    IncomeAddService incomeAddService = Mock(IncomeAddService)
    GroupAddService groupAddService = Mock(GroupAddService)

    OnboardingService service

    def setup() {
        service = new OnboardingService(userAddService, incomeAddService, groupAddService)
    }

    def "finish - should create user, accounts and income when all fields are present"() {
        given:
        def amount = new OnBoardingAmount(new BigDecimal("1500.00"), "DEFAULT", "GALICIA", "ARS")
        def form = new OnBoardingForm(amount, "CONSUMER", ["Viajes", "Casa"])
        def user = Stub(User) { getId() >> 42L }

        userAddService.createLogInUser() >> user

        when:
        service.finish(form)

        then:
        1 * groupAddService.createAccount(new AddGroupRecord("Viajes"))
        1 * groupAddService.createAccount(new AddGroupRecord("Casa"))
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
        def form = new OnBoardingForm(amount, "CONSUMER", ["Hogar"])
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
        def form = new OnBoardingForm(amount, "COMPANY", ["Gastos"])
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
        def form = new OnBoardingForm(amount, "CONSUMER", ["Personal"])
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
        def form = new OnBoardingForm(amount, "CONSUMER", ["Alpha", "Beta", "Gamma"])
        def user = Stub(User) { getId() >> 10L }

        userAddService.createLogInUser() >> user

        when:
        service.finish(form)

        then:
        1 * groupAddService.createAccount(new AddGroupRecord("Alpha"))
        1 * groupAddService.createAccount(new AddGroupRecord("Beta"))
        1 * groupAddService.createAccount(new AddGroupRecord("Gamma"))
    }
}
