package api.m2.movements.unit.services

import api.m2.movements.entities.Budget
import api.m2.movements.entities.commons.Category
import api.m2.movements.entities.commons.Currency
import api.m2.movements.enums.NotificationSeverity
import api.m2.movements.records.categories.CategoryRecord
import api.m2.movements.records.currencies.CurrencyRecord
import api.m2.movements.records.movements.MovementRecord
import api.m2.movements.records.workspaces.WorkspaceBaseRecord
import api.m2.movements.repositories.BudgetRepository
import api.m2.movements.services.budgets.BudgetThresholdEventHandler
import api.m2.movements.services.notifications.NotificationService
import spock.lang.Specification

import java.time.LocalDate

class BudgetThresholdEventHandlerTest extends Specification {

    BudgetRepository budgetRepository = Mock(BudgetRepository)
    NotificationService notificationService = Mock(NotificationService)

    BudgetThresholdEventHandler handler

    def setup() {
        handler = new BudgetThresholdEventHandler(budgetRepository, notificationService)
    }

    def buildMovementRecord(BigDecimal amount, String type = "DEBITO", LocalDate date = LocalDate.of(2026, 8, 15)) {
        def category = new CategoryRecord(1L, "Comida", true, true, null, null)
        def currency = new CurrencyRecord("ARS", 1L)
        def metadata = new MovementRecord.Metadata(null, new WorkspaceBaseRecord(1L, "Familia"), null, null)
        return new MovementRecord(1L, amount, "Super", date, null, null,
                [category], currency, null, type, null, null, null, metadata)
    }

    def buildBudget(BigDecimal amount, Integer year = null, Integer month = null) {
        def category = Stub(Category) { getDescription() >> "Comida" }
        def currency = Stub(Currency) { getSymbol() >> "ARS" }
        return new Budget(id: 10L, workspaceId: 1L, category: category, currency: currency,
                amount: amount, year: year, month: month)
    }

    def "onMovementAdded - should notify when the movement makes spending cross the budget limit"() {
        given:
        def record = buildMovementRecord(new BigDecimal("10000"))
        def budget = buildBudget(new BigDecimal("40000"))
        budgetRepository.findByWorkspaceCategoryDescriptionAndCurrency(1L, "Comida", "ARS") >> Optional.of(budget)
        budgetRepository.sumSpentByCategoryAndPeriod(1L, "Comida", "ARS", 2026, 8) >> new BigDecimal("45000")

        when:
        handler.onMovementAdded(record)

        then:
        1 * notificationService.publish(1L, "Presupuesto superado", "Comida — \$45000/\$40000", NotificationSeverity.WARNING)
    }

    def "onMovementAdded - should not notify again when the budget was already over the limit"() {
        given:
        def record = buildMovementRecord(new BigDecimal("5000"))
        def budget = buildBudget(new BigDecimal("40000"))
        budgetRepository.findByWorkspaceCategoryDescriptionAndCurrency(1L, "Comida", "ARS") >> Optional.of(budget)
        budgetRepository.sumSpentByCategoryAndPeriod(1L, "Comida", "ARS", 2026, 8) >> new BigDecimal("50000")

        when:
        handler.onMovementAdded(record)

        then:
        0 * notificationService.publish(_ as Long, _ as String, _ as String, _ as NotificationSeverity)
    }

    def "onMovementAdded - should not notify when spending stays under the limit"() {
        given:
        def record = buildMovementRecord(new BigDecimal("5000"))
        def budget = buildBudget(new BigDecimal("40000"))
        budgetRepository.findByWorkspaceCategoryDescriptionAndCurrency(1L, "Comida", "ARS") >> Optional.of(budget)
        budgetRepository.sumSpentByCategoryAndPeriod(1L, "Comida", "ARS", 2026, 8) >> new BigDecimal("30000")

        when:
        handler.onMovementAdded(record)

        then:
        0 * notificationService.publish(_ as Long, _ as String, _ as String, _ as NotificationSeverity)
    }

    def "onMovementAdded - should ignore INGRESO movements"() {
        given:
        def record = buildMovementRecord(new BigDecimal("10000"), "INGRESO")

        when:
        handler.onMovementAdded(record)

        then:
        0 * budgetRepository.findByWorkspaceCategoryDescriptionAndCurrency(_ as Long, _ as String, _ as String)
        0 * notificationService.publish(_ as Long, _ as String, _ as String, _ as NotificationSeverity)
    }

    def "onMovementAdded - should do nothing when no budget is configured for the category and currency"() {
        given:
        def record = buildMovementRecord(new BigDecimal("10000"))
        budgetRepository.findByWorkspaceCategoryDescriptionAndCurrency(1L, "Comida", "ARS") >> Optional.empty()

        when:
        handler.onMovementAdded(record)

        then:
        0 * notificationService.publish(_ as Long, _ as String, _ as String, _ as NotificationSeverity)
    }

    def "onMovementAdded - should use the yearly sum for annual budgets"() {
        given:
        def record = buildMovementRecord(new BigDecimal("10000"))
        def budget = buildBudget(new BigDecimal("100000"), 2026, null)
        budgetRepository.findByWorkspaceCategoryDescriptionAndCurrency(1L, "Comida", "ARS") >> Optional.of(budget)
        budgetRepository.sumSpentByCategoryAndYear(1L, "Comida", "ARS", 2026) >> new BigDecimal("105000")

        when:
        handler.onMovementAdded(record)

        then:
        1 * notificationService.publish(1L, "Presupuesto superado", "Comida — \$105000/\$100000", NotificationSeverity.WARNING)
    }

    def "onMovementAdded - should skip a one-off budget that does not match the movement's period"() {
        given:
        def record = buildMovementRecord(new BigDecimal("10000"))
        def budget = buildBudget(new BigDecimal("40000"), 2026, 3)
        budgetRepository.findByWorkspaceCategoryDescriptionAndCurrency(1L, "Comida", "ARS") >> Optional.of(budget)

        when:
        handler.onMovementAdded(record)

        then:
        0 * budgetRepository.sumSpentByCategoryAndPeriod(_ as Long, _ as String, _ as String, _ as int, _ as int)
        0 * notificationService.publish(_ as Long, _ as String, _ as String, _ as NotificationSeverity)
    }
}
