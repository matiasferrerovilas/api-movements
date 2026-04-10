package api.m2.movements.unit.services

import api.m2.movements.entities.Account
import api.m2.movements.entities.Budget
import api.m2.movements.entities.Category
import api.m2.movements.entities.Currency
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.mappers.BudgetMapper
import api.m2.movements.records.BudgetToAdd
import api.m2.movements.records.budgets.BudgetToUpdate
import api.m2.movements.repositories.BudgetRepository
import api.m2.movements.repositories.CategoryRepository
import api.m2.movements.repositories.CurrencyRepository
import api.m2.movements.services.budgets.BudgetAddService
import api.m2.movements.services.groups.AccountQueryService
import spock.lang.Specification

class BudgetAddServiceTest extends Specification {

    BudgetRepository budgetRepository = Mock()
    BudgetMapper budgetMapper = Mock()
    CategoryRepository categoryRepository = Mock()
    CurrencyRepository currencyRepository = Mock()
    AccountQueryService accountQueryService = Mock()

    BudgetAddService service

    def setup() {
        service = new BudgetAddService(
                budgetRepository,
                budgetMapper,
                categoryRepository,
                currencyRepository,
                accountQueryService
        )
    }

    // --- save ---

    def "save - should create budget for the resolved account"() {
        given:
        def dto = new BudgetToAdd(1L, "Supermercado", "ARS", new BigDecimal("5000.00"), null, null)
        def account = Stub(Account) { getId() >> 1L }
        def budget = Mock(Budget)

        accountQueryService.findAccountById(1L) >> account
        budgetMapper.toEntity(dto, categoryRepository, currencyRepository) >> budget

        when:
        service.save(dto)

        then:
        1 * budget.setAccount(account)
        1 * budgetRepository.save(budget)
    }

    def "save - should delegate account resolution to AccountQueryService"() {
        given:
        def dto = new BudgetToAdd(99L, "Hogar", "USD", new BigDecimal("200.00"), 2026, 4)
        def account = Stub(Account) { getId() >> 99L }
        def budget = Stub(Budget)

        budgetMapper.toEntity(dto, categoryRepository, currencyRepository) >> budget

        when:
        service.save(dto)

        then:
        1 * accountQueryService.findAccountById(99L) >> account
    }

    // --- update ---

    def "update - should update amount when budget exists"() {
        given:
        def currency = Stub(Currency) { getId() >> 1L; getSymbol() >> "ARS" }
        def account = Stub(Account) { getId() >> 1L }
        def category = Stub(Category) { getId() >> 1L; getDescription() >> "Hogar" }
        def budget = new Budget(id: 10L, account: account, currency: currency,
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
