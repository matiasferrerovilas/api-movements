package api.m2.movements.unit.services

import api.m2.movements.entities.Bank
import api.m2.movements.entities.Currency
import api.m2.movements.entities.Income
import api.m2.movements.entities.User
import api.m2.movements.entities.Workspace
import api.m2.movements.mappers.IncomeMapper
import api.m2.movements.repositories.IncomeRepository
import api.m2.movements.services.income.IncomeQueryService
import api.m2.movements.services.user.UserService
import org.mapstruct.factory.Mappers
import spock.lang.Specification

class IncomeQueryServiceTest extends Specification {

    IncomeMapper incomeMapper = Mappers.getMapper(IncomeMapper)
    IncomeRepository incomeRepository = Mock(IncomeRepository)
    UserService userService = Mock(UserService)

    IncomeQueryService service

    def setup() {
        service = new IncomeQueryService(
                incomeRepository,
                userService,
                incomeMapper
        )
    }

    def "getAllIncomes - should return mapped income records for authenticated user"() {
        given:
        def userId = 1L
        def user = Stub(User) { getId() >> userId }

        def workspace = Stub(Workspace) { getId() >> 10L; getName() >> "Mi cuenta" }
        def currency = Stub(Currency) { getSymbol() >> "ARS"; getId() >> 1L }
        def bank = Stub(Bank) { getId() >> 1L; getDescription() >> "GALICIA" }

        def income = new Income(
                id: 1L,
                amount: new BigDecimal("50000.00"),
                user: user,
                workspace: workspace,
                currency: currency,
                bank: bank
        )

        userService.getAuthenticatedUser() >> user
        incomeRepository.findAllByUserOrGroupsIn(userId) >> [income]

        when:
        def result = service.getAllIncomes()

        then:
        result.size() == 1
        result[0].amount() == new BigDecimal("50000.00")
        result[0].currency().symbol() == "ARS"
        result[0].bank() == "GALICIA"
    }

    def "getAllIncomes - should return empty list when user has no incomes"() {
        given:
        def userId = 1L
        def user = Stub(User) { getId() >> userId }

        userService.getAuthenticatedUser() >> user
        incomeRepository.findAllByUserOrGroupsIn(userId) >> []

        when:
        def result = service.getAllIncomes()

        then:
        result.isEmpty()
    }

    def "getAllIncomes - should return multiple incomes for user"() {
        given:
        def userId = 1L
        def user = Stub(User) { getId() >> userId }

        def workspace = Stub(Workspace) { getId() >> 10L; getName() >> "Mi cuenta" }
        def currencyArs = Stub(Currency) { getSymbol() >> "ARS"; getId() >> 1L }
        def currencyUsd = Stub(Currency) { getSymbol() >> "USD"; getId() >> 2L }
        def bank = Stub(Bank) { getId() >> 1L; getDescription() >> "BBVA" }

        def income1 = new Income(
                id: 1L,
                amount: new BigDecimal("50000.00"),
                user: user,
                workspace: workspace,
                currency: currencyArs,
                bank: bank
        )
        def income2 = new Income(
                id: 2L,
                amount: new BigDecimal("1000.00"),
                user: user,
                workspace: workspace,
                currency: currencyUsd,
                bank: bank
        )

        userService.getAuthenticatedUser() >> user
        incomeRepository.findAllByUserOrGroupsIn(userId) >> [income1, income2]

        when:
        def result = service.getAllIncomes()

        then:
        result.size() == 2
        result.collect { it.currency().symbol() } as Set == ["ARS", "USD"] as Set
    }
}
