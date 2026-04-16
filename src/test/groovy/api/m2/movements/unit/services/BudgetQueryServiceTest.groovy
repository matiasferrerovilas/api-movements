package api.m2.movements.unit.services

import api.m2.movements.entities.Budget
import api.m2.movements.entities.Category
import api.m2.movements.entities.Currency
import api.m2.movements.entities.Workspace
import api.m2.movements.mappers.BudgetMapper
import api.m2.movements.records.budgets.BudgetRecord
import api.m2.movements.records.categories.CategoryRecord
import api.m2.movements.records.currencies.CurrencyRecord
import api.m2.movements.repositories.BudgetRepository
import api.m2.movements.services.budgets.BudgetQueryService
import api.m2.movements.services.workspaces.WorkspaceContextService
import spock.lang.Specification

class BudgetQueryServiceTest extends Specification {

    BudgetRepository budgetRepository = Mock()
    BudgetMapper budgetMapper = Mock()
    WorkspaceContextService workspaceContextService = Mock()

    BudgetQueryService service

    def setup() {
        service = new BudgetQueryService(budgetRepository, budgetMapper, workspaceContextService)
    }

    def buildBudget(String categoryName, String currencySymbol, BigDecimal amount) {
        def workspace = Stub(Workspace) { getId() >> 1L }
        def category = categoryName == null ? null : Stub(Category) {
            getId() >> 1L
            getDescription() >> categoryName
            isDeletable() >> false
        }
        def currency = Stub(Currency) { getId() >> 1L; getSymbol() >> currencySymbol }
        return Stub(Budget) {
            getId() >> 1L
            getWorkspace() >> workspace
            getCategory() >> category
            getCurrency() >> currency
            getAmount() >> amount
            getYear() >> null
            getMonth() >> null
        }
    }

    // --- getByAccount ---

    def "getByAccount - should return budgets with spent calculated"() {
        given:
        def budget = buildBudget("Supermercado", "ARS", new BigDecimal("5000.00"))
        def expectedRecord = new BudgetRecord(1L, 1L,
                new CategoryRecord(1L, "Supermercado", true, false, null, null),
                new CurrencyRecord("ARS", 1L),
                new BigDecimal("5000.00"), null, null,
                new BigDecimal("2000.00"), new BigDecimal("40.00"))

        workspaceContextService.getActiveWorkspaceId() >> 1L
        budgetRepository.findByAccountAndPeriod(1L, "ARS", 2026, 4) >> [budget]
        budgetRepository.sumSpentByCategoryAndPeriod(1L, "Supermercado", "ARS", 2026, 4) >> new BigDecimal("2000.00")
        budgetMapper.toRecordWithSpent(budget, new BigDecimal("2000.00")) >> expectedRecord

        when:
        def result = service.getByAccount("ARS", 2026, 4)

        then:
        result.size() == 1
        result[0].spent() == new BigDecimal("2000.00")
        result[0].percentage() == new BigDecimal("40.00")
    }

    def "getByAccount - should return empty list when no budgets found"() {
        given:
        workspaceContextService.getActiveWorkspaceId() >> 1L
        budgetRepository.findByAccountAndPeriod(1L, "ARS", 2026, 4) >> []

        when:
        def result = service.getByAccount("ARS", 2026, 4)

        then:
        result.isEmpty()
        0 * budgetMapper.toRecordWithSpent(_ as Budget, _ as BigDecimal)
    }

    def "getByAccount - should use zero as spent when category is null"() {
        given:
        def budget = buildBudget(null, "ARS", new BigDecimal("3000.00"))
        def expectedRecord = new BudgetRecord(1L, 1L, null,
                new CurrencyRecord("ARS", 1L),
                new BigDecimal("3000.00"), null, null,
                BigDecimal.ZERO, BigDecimal.ZERO)

        workspaceContextService.getActiveWorkspaceId() >> 1L
        budgetRepository.findByAccountAndPeriod(1L, "ARS", 2026, 4) >> [budget]
        budgetMapper.toRecordWithSpent(budget, BigDecimal.ZERO) >> expectedRecord

        when:
        def result = service.getByAccount("ARS", 2026, 4)

        then:
        result.size() == 1
        0 * budgetRepository.sumSpentByCategoryAndPeriod(_, _, _, _, _)
    }

    def "getByAccount - should treat null DB result as zero spent"() {
        given:
        def budget = buildBudget("Hogar", "USD", new BigDecimal("500.00"))
        def expectedRecord = new BudgetRecord(1L, 1L,
                new CategoryRecord(1L, "Hogar", true, false, null, null),
                new CurrencyRecord("USD", 1L),
                new BigDecimal("500.00"), null, null,
                BigDecimal.ZERO, BigDecimal.ZERO)

        workspaceContextService.getActiveWorkspaceId() >> 1L
        budgetRepository.findByAccountAndPeriod(1L, "USD", 2026, 4) >> [budget]
        budgetRepository.sumSpentByCategoryAndPeriod(1L, "Hogar", "USD", 2026, 4) >> null
        budgetMapper.toRecordWithSpent(budget, BigDecimal.ZERO) >> expectedRecord

        when:
        def result = service.getByAccount("USD", 2026, 4)

        then:
        result.size() == 1
        1 * budgetMapper.toRecordWithSpent(budget, BigDecimal.ZERO) >> expectedRecord
    }

    def "getByAccount - should return all budgets when currency is null"() {
        given:
        def budgetArs = buildBudget("Supermercado", "ARS", new BigDecimal("5000.00"))
        def budgetUsd = buildBudget("Hogar", "USD", new BigDecimal("500.00"))
        def expectedRecordArs = new BudgetRecord(1L, 1L,
                new CategoryRecord(1L, "Supermercado", true, false, null, null),
                new CurrencyRecord("ARS", 1L),
                new BigDecimal("5000.00"), null, null,
                new BigDecimal("2000.00"), new BigDecimal("40.00"))
        def expectedRecordUsd = new BudgetRecord(1L, 1L,
                new CategoryRecord(1L, "Hogar", true, false, null, null),
                new CurrencyRecord("USD", 1L),
                new BigDecimal("500.00"), null, null,
                new BigDecimal("100.00"), new BigDecimal("20.00"))

        workspaceContextService.getActiveWorkspaceId() >> 1L
        budgetRepository.findByWorkspaceAndPeriod(1L, 2026, 4) >> [budgetArs, budgetUsd]
        budgetRepository.sumSpentByCategoryAndPeriod(1L, "Supermercado", "ARS", 2026, 4) >> new BigDecimal("2000.00")
        budgetRepository.sumSpentByCategoryAndPeriod(1L, "Hogar", "USD", 2026, 4) >> new BigDecimal("100.00")
        budgetMapper.toRecordWithSpent(budgetArs, new BigDecimal("2000.00")) >> expectedRecordArs
        budgetMapper.toRecordWithSpent(budgetUsd, new BigDecimal("100.00")) >> expectedRecordUsd

        when:
        def result = service.getByAccount(null, 2026, 4)

        then:
        result.size() == 2
        0 * budgetRepository.findByAccountAndPeriod(_, _, _, _)
    }

    def "getByAccount - should call findByWorkspaceAndPeriod when currency is null"() {
        given:
        workspaceContextService.getActiveWorkspaceId() >> 1L

        when:
        def result = service.getByAccount(null, 2026, 4)

        then:
        1 * budgetRepository.findByWorkspaceAndPeriod(1L, 2026, 4) >> []
        0 * budgetRepository.findByAccountAndPeriod(_, _, _, _)
        result.isEmpty()
    }
}
