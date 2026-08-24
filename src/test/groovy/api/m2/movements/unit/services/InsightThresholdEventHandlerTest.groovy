package api.m2.movements.unit.services

import api.m2.movements.enums.InsightDirection
import api.m2.movements.enums.NotificationSeverity
import api.m2.movements.records.categories.CategoryRecord
import api.m2.movements.records.currencies.CurrencyRecord
import api.m2.movements.records.insights.CategoryInsightRecord
import api.m2.movements.records.movements.MovementRecord
import api.m2.movements.records.workspaces.WorkspaceBaseRecord
import api.m2.movements.repositories.BudgetRepository
import api.m2.movements.services.insights.InsightService
import api.m2.movements.services.insights.InsightThresholdEventHandler
import api.m2.movements.services.notifications.NotificationService
import spock.lang.Specification

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class InsightThresholdEventHandlerTest extends Specification {

    BudgetRepository budgetRepository = Mock(BudgetRepository)
    InsightService insightService = Mock(InsightService)
    NotificationService notificationService = Mock(NotificationService)

    // "Ahora" fijo en 2026-08-15, mismo mes que los movimientos de prueba
    Clock clock = Clock.fixed(Instant.parse("2026-08-15T12:00:00Z"), ZoneOffset.UTC)

    InsightThresholdEventHandler handler

    def setup() {
        handler = new InsightThresholdEventHandler(budgetRepository, insightService, notificationService, clock)
    }

    def buildMovementRecord(BigDecimal amount, String type = "DEBITO", LocalDate date = LocalDate.of(2026, 8, 15)) {
        def category = new CategoryRecord(1L, "Supermercado", true, true, null, null)
        def currency = new CurrencyRecord("ARS", 1L)
        def metadata = new MovementRecord.Metadata(null, new WorkspaceBaseRecord(1L, "Familia"), null, null)
        return new MovementRecord(1L, amount, "Compra", date, null, null,
                [category], currency, null, type, null, null, metadata)
    }

    def "onMovementAdded - should notify when the movement makes the category cross the insight threshold"() {
        given:
        def record = buildMovementRecord(new BigDecimal("500"))
        budgetRepository.sumSpentByCategoryAndPeriod(1L, "Supermercado", "ARS", 2026, 8) >> new BigDecimal("2000")
        insightService.evaluateCategory(1L, "Supermercado", "ARS", new BigDecimal("1500")) >> Optional.empty()
        insightService.evaluateCategory(1L, "Supermercado", "ARS", new BigDecimal("2000")) >>
                Optional.of(new CategoryInsightRecord("Supermercado", new CurrencyRecord("ARS", 1L), new BigDecimal("2000"),
                        new BigDecimal("1000"), new BigDecimal("100.00"), InsightDirection.ABOVE))

        when:
        handler.onMovementAdded(record)

        then:
        1 * notificationService.publish(1L, "Gasto fuera de lo normal",
                "Supermercado — +100.00% vs. promedio de 6 meses", NotificationSeverity.INFO)
    }

    def "onMovementAdded - should not notify again when the category was already flagged"() {
        given:
        def record = buildMovementRecord(new BigDecimal("100"))
        budgetRepository.sumSpentByCategoryAndPeriod(1L, "Supermercado", "ARS", 2026, 8) >> new BigDecimal("2100")
        insightService.evaluateCategory(1L, "Supermercado", "ARS", new BigDecimal("2000")) >>
                Optional.of(new CategoryInsightRecord("Supermercado", new CurrencyRecord("ARS", 1L), new BigDecimal("2000"),
                        new BigDecimal("1000"), new BigDecimal("100.00"), InsightDirection.ABOVE))

        when:
        handler.onMovementAdded(record)

        then:
        0 * notificationService.publish(_ as Long, _ as String, _ as String, _ as NotificationSeverity)
    }

    def "onMovementAdded - should not notify when the category stays within the normal range"() {
        given:
        def record = buildMovementRecord(new BigDecimal("50"))
        budgetRepository.sumSpentByCategoryAndPeriod(1L, "Supermercado", "ARS", 2026, 8) >> new BigDecimal("1050")
        insightService.evaluateCategory(1L, "Supermercado", "ARS", new BigDecimal("1000")) >> Optional.empty()
        insightService.evaluateCategory(1L, "Supermercado", "ARS", new BigDecimal("1050")) >> Optional.empty()

        when:
        handler.onMovementAdded(record)

        then:
        0 * notificationService.publish(_ as Long, _ as String, _ as String, _ as NotificationSeverity)
    }

    def "onMovementAdded - should ignore INGRESO movements"() {
        given:
        def record = buildMovementRecord(new BigDecimal("500"), "INGRESO")

        when:
        handler.onMovementAdded(record)

        then:
        0 * budgetRepository.sumSpentByCategoryAndPeriod(_ as Long, _ as String, _ as String, _ as int, _ as int)
        0 * notificationService.publish(_ as Long, _ as String, _ as String, _ as NotificationSeverity)
    }

    def "onMovementAdded - should ignore movements backdated outside the current month"() {
        given:
        def record = buildMovementRecord(new BigDecimal("500"), "DEBITO", LocalDate.of(2026, 3, 1))

        when:
        handler.onMovementAdded(record)

        then:
        0 * budgetRepository.sumSpentByCategoryAndPeriod(_ as Long, _ as String, _ as String, _ as int, _ as int)
        0 * notificationService.publish(_ as Long, _ as String, _ as String, _ as NotificationSeverity)
    }
}
