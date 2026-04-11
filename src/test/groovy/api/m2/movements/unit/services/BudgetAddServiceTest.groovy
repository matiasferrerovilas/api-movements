package api.m2.movements.unit.services

import api.m2.movements.entities.Budget
import api.m2.movements.entities.Category
import api.m2.movements.entities.Currency
import api.m2.movements.entities.Workspace
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.mappers.BudgetMapper
import api.m2.movements.records.BudgetToAdd
import api.m2.movements.records.budgets.BudgetToUpdate
import api.m2.movements.repositories.BudgetRepository
import api.m2.movements.repositories.CategoryRepository
import api.m2.movements.repositories.CurrencyRepository
import api.m2.movements.services.budgets.BudgetAddService
import api.m2.movements.services.workspaces.WorkspaceQueryService
import spock.lang.Specification

class BudgetAddServiceTest extends Specification {

    BudgetRepository budgetRepository = Mock()
    BudgetMapper budgetMapper = Mock()
    CategoryRepository categoryRepository = Mock()
    CurrencyRepository currencyRepository = Mock()
    WorkspaceQueryService workspaceQueryService = Mock()

    BudgetAddService service

    def setup() {
        service = new BudgetAddService(
                budgetRepository,
                budgetMapper,
                categoryRepository,
                currencyRepository,
                workspaceQueryService
        )
    }

    // --- save ---

    def "save - should create budget for the resolved workspace"() {
        given:
        def dto = new BudgetToAdd(1L, "Supermercado", "ARS", new BigDecimal("5000.00"), null, null)
        def workspace = Stub(Workspace) { getId() >> 1L }
        def budget = Mock(Budget)

        workspaceQueryService.findWorkspaceById(1L) >> workspace
        budgetMapper.toEntity(dto, categoryRepository, currencyRepository) >> budget

        when:
        service.save(dto)

        then:
        1 * budget.setWorkspace(workspace)
        1 * budgetRepository.save(budget)
    }

    def "save - should delegate workspace resolution to WorkspaceQueryService"() {
        given:
        def dto = new BudgetToAdd(99L, "Hogar", "USD", new BigDecimal("200.00"), 2026, 4)
        def workspace = Stub(Workspace) { getId() >> 99L }
        def budget = Stub(Budget)

        budgetMapper.toEntity(dto, categoryRepository, currencyRepository) >> budget

        when:
        service.save(dto)

        then:
        1 * workspaceQueryService.findWorkspaceById(99L) >> workspace
    }

    // --- update ---

    def "update - should update amount when budget exists"() {
        given:
        def currency = Stub(Currency) { getId() >> 1L; getSymbol() >> "ARS" }
        def workspace = Stub(Workspace) { getId() >> 1L }
        def category = Stub(Category) { getId() >> 1L; getDescription() >> "Hogar" }
        def budget = new Budget(id: 10L, workspace: workspace, currency: currency,
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
