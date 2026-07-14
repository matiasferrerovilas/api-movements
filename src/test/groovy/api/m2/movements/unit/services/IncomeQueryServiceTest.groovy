package api.m2.movements.unit.services

import api.m2.movements.movements.entities.commons.Bank
import api.m2.movements.movements.entities.commons.Currency
import api.m2.movements.movements.entities.movements.Income

import api.m2.movements.movements.mappers.IncomeMapper
import api.m2.movements.movements.repositories.IncomeRepository
import api.m2.movements.movements.services.income.IncomeQueryService
import api.m2.movements.identity.services.workspaces.WorkspaceContextService
import org.mapstruct.factory.Mappers
import spock.lang.Specification

class IncomeQueryServiceTest extends Specification {

    IncomeMapper incomeMapper = Mappers.getMapper(IncomeMapper)
    IncomeRepository incomeRepository = Mock(IncomeRepository)
    WorkspaceContextService workspaceContextService = Mock(WorkspaceContextService)

    IncomeQueryService service

    def setup() {
        service = new IncomeQueryService(
                incomeRepository,
                workspaceContextService,
                incomeMapper
        )
    }

    def "getAllIncomes - should return mapped income records for authenticated user"() {
        given:
        def workspaceId = 10L

        def currency = Stub(Currency) { getSymbol() >> "ARS"; getId() >> 1L }
        def bank = Stub(Bank) { getId() >> 1L; getDescription() >> "GALICIA" }

        def income = new Income(
                id: 1L,
                amount: new BigDecimal("50000.00"),
                userId: 1L,
                workspaceId: workspaceId,
                currency: currency,
                bank: bank
        )

        workspaceContextService.getActiveWorkspaceId() >> workspaceId
        incomeRepository.findAllByWorkspaceId(workspaceId) >> [income]

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
        def workspaceId = 10L

        workspaceContextService.getActiveWorkspaceId() >> workspaceId
        incomeRepository.findAllByWorkspaceId(workspaceId) >> []

        when:
        def result = service.getAllIncomes()

        then:
        result.isEmpty()
    }

    def "getAllIncomes - should return multiple incomes for user"() {
        given:
        def workspaceId = 10L

        def currencyArs = Stub(Currency) { getSymbol() >> "ARS"; getId() >> 1L }
        def currencyUsd = Stub(Currency) { getSymbol() >> "USD"; getId() >> 2L }
        def bank = Stub(Bank) { getId() >> 1L; getDescription() >> "BBVA" }

        def income1 = new Income(
                id: 1L,
                amount: new BigDecimal("50000.00"),
                userId: 1L,
                workspaceId: workspaceId,
                currency: currencyArs,
                bank: bank
        )
        def income2 = new Income(
                id: 2L,
                amount: new BigDecimal("1000.00"),
                userId: 1L,
                workspaceId: workspaceId,
                currency: currencyUsd,
                bank: bank
        )

        workspaceContextService.getActiveWorkspaceId() >> workspaceId
        incomeRepository.findAllByWorkspaceId(workspaceId) >> [income1, income2]

        when:
        def result = service.getAllIncomes()

        then:
        result.size() == 2
        result.collect { it.currency().symbol() } as Set == ["ARS", "USD"] as Set
    }
}
