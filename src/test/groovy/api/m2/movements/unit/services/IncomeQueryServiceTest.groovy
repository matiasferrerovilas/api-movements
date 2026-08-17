package api.m2.movements.unit.services

import api.m2.movements.entities.commons.Bank
import api.m2.movements.entities.commons.Currency
import api.m2.movements.entities.movements.Income

import api.m2.movements.mappers.IncomeMapper
import api.m2.movements.repositories.IncomeRepository
import api.m2.movements.services.income.IncomeQueryService
import api.m2.movements.services.workspaces.WorkspaceContextService
import org.mapstruct.factory.Mappers
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import spock.lang.Specification

class IncomeQueryServiceTest extends Specification {

    IncomeMapper incomeMapper = Mappers.getMapper(IncomeMapper)
    IncomeRepository incomeRepository = Mock(IncomeRepository)
    WorkspaceContextService workspaceContextService = Mock(WorkspaceContextService)

    IncomeQueryService service

    def pageable = PageRequest.of(0, 20)

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
        incomeRepository.findAllByWorkspaceId(workspaceId, pageable) >> new PageImpl<>([income], pageable, 1)

        when:
        def result = service.getAllIncomes(pageable)

        then:
        result.getContent().size() == 1
        result.getContent()[0].amount() == new BigDecimal("50000.00")
        result.getContent()[0].currency().symbol() == "ARS"
        result.getContent()[0].bank() == "GALICIA"
    }

    def "getAllIncomes - should return empty page when user has no incomes"() {
        given:
        def workspaceId = 10L

        workspaceContextService.getActiveWorkspaceId() >> workspaceId
        incomeRepository.findAllByWorkspaceId(workspaceId, pageable) >> new PageImpl<>([], pageable, 0)

        when:
        def result = service.getAllIncomes(pageable)

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
        incomeRepository.findAllByWorkspaceId(workspaceId, pageable) >> new PageImpl<>([income1, income2], pageable, 2)

        when:
        def result = service.getAllIncomes(pageable)

        then:
        result.getContent().size() == 2
        result.getContent().collect { it.currency().symbol() } as Set == ["ARS", "USD"] as Set
    }
}
