package api.m2.movements.unit.services

import api.m2.movements.movements.entities.Budget
import api.m2.movements.movements.entities.commons.Category
import api.m2.movements.movements.entities.commons.Currency
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.movements.mappers.BudgetMapper
import api.m2.movements.movements.records.BudgetToAdd
import api.m2.movements.movements.records.budgets.BudgetToUpdate
import api.m2.movements.movements.repositories.BudgetRepository
import api.m2.movements.movements.repositories.CategoryRepository
import api.m2.movements.movements.repositories.CurrencyRepository
import api.m2.movements.movements.services.budgets.BudgetAddService
import api.m2.movements.identity.services.workspaces.WorkspaceContextService
import spock.lang.Specification

class BudgetAddServiceTest extends Specification {

    BudgetRepository budgetRepository = Mock()
    BudgetMapper budgetMapper = Mock()
    CategoryRepository categoryRepository = Mock()
    CurrencyRepository currencyRepository = Mock()
    WorkspaceContextService workspaceContextService = Mock()

    BudgetAddService service

    def setup() {
        service = new BudgetAddService(
                budgetRepository,
                budgetMapper,
                categoryRepository,
                currencyRepository,
                workspaceContextService
        )
    }

    // --- save ---

    def "save - should create budget for the resolved workspace"() {
        given:
        def dto = new BudgetToAdd("Supermercado", "ARS", new BigDecimal("5000.00"), null, null)
        def budget = Mock(Budget)

        workspaceContextService.getActiveWorkspaceId() >> 1L
        budgetMapper.toEntity(dto, categoryRepository, currencyRepository) >> budget

        when:
        service.save(dto)

        then:
        1 * budget.setWorkspaceId(1L)
        1 * budgetRepository.save(budget)
    }

    def "save - should delegate workspace resolution to WorkspaceContextService"() {
        given:
        def dto = new BudgetToAdd("Hogar", "USD", new BigDecimal("200.00"), 2026, 4)
        def budget = Stub(Budget)

        budgetMapper.toEntity(dto, categoryRepository, currencyRepository) >> budget

        when:
        service.save(dto)

        then:
        1 * workspaceContextService.getActiveWorkspaceId() >> 99L
    }

    // --- update ---

    def "update - should update amount when budget exists"() {
        given:
        def currency = Stub(Currency) { getId() >> 1L; getSymbol() >> "ARS" }
        def category = Stub(Category) { getId() >> 1L; getDescription() >> "Hogar" }
        def budget = new Budget(id: 10L, workspaceId: 1L, currency: currency,
                category: category, amount: new BigDecimal("1000.00"))
        def dto = new BudgetToUpdate(new BigDecimal("1500.00"))

        budgetRepository.findById(10L) >> Optional.of(budget)

        when:
        service.update(dto, 10L)

        then:
        1 * budgetRepository.save(budget)
        budget.amount == new BigDecimal("1500.00")
    }

    def "update - should throw EntityNotFoundException when budget does not exist"() {
        given:
        budgetRepository.findById(999L) >> Optional.empty()

        when:
        service.update(new BudgetToUpdate(new BigDecimal("500.00")), 999L)

        then:
        thrown(EntityNotFoundException)
        0 * budgetRepository.save(_ as Budget)
    }

    // --- delete ---

    def "delete - should remove budget when it exists"() {
        given:
        budgetRepository.existsById(5L) >> true

        when:
        service.delete(5L)

        then:
        1 * budgetRepository.deleteById(5L)
    }

    def "delete - should throw EntityNotFoundException when budget does not exist"() {
        given:
        budgetRepository.existsById(999L) >> false

        when:
        service.delete(999L)

        then:
        thrown(EntityNotFoundException)
        0 * budgetRepository.deleteById(_ as Long)
    }
}
