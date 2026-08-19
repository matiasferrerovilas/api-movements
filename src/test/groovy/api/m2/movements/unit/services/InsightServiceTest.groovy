package api.m2.movements.unit.services

import api.m2.movements.clients.identity.response.UserMe
import api.m2.movements.enums.InsightDirection
import api.m2.movements.records.balance.CategoryAmountRecord
import api.m2.movements.records.balance.MonthlySummaryByCurrencyRecord
import api.m2.movements.records.balance.MonthlySummaryComparisonRecord
import api.m2.movements.records.balance.MonthlySummaryResponse
import api.m2.movements.services.balance.MonthlySummaryService
import api.m2.movements.services.insights.InsightService
import api.m2.movements.services.user.UserService
import api.m2.movements.services.workspaces.WorkspaceQueryService
import spock.lang.Specification

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class InsightServiceTest extends Specification {

    MonthlySummaryService monthlySummaryService = Mock()
    WorkspaceQueryService workspaceQueryService = Mock()
    UserService userService = Mock()

    // "Ahora" fijo en 2025-06-15 → mes actual = 2025-06, historial = 2025-05 .. 2024-12
    Clock clock = Clock.fixed(Instant.parse("2025-06-15T12:00:00Z"), ZoneOffset.UTC)

    InsightService service

    def user = new UserMe(1L, "user@test.com", "User", null, "PERSONAL", null)
    def workspaceId = 10L

    def setup() {
        service = new InsightService(monthlySummaryService, workspaceQueryService, userService, clock)
        userService.getMe() >> user
    }

    private static MonthlySummaryComparisonRecord emptyComparison() {
        new MonthlySummaryComparisonRecord(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
    }

    private static MonthlySummaryResponse summaryWith(int year, int month, String currency,
                                                        List<CategoryAmountRecord> categories) {
        def byCurrency = new MonthlySummaryByCurrencyRecord(
                currency, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, emptyComparison(), categories
        )
        new MonthlySummaryResponse(year, month, null, [byCurrency])
    }

    def "getInsights - should verify membership before returning insights"() {
        given:
        monthlySummaryService.getSummary(workspaceId, _ as Integer, _ as Integer) >>
                summaryWith(2025, 6, "ARS", [])

        when:
        service.getInsights(workspaceId)

        then:
        1 * workspaceQueryService.verifyUserIsMemberOfWorkspace(workspaceId, 1L)
    }

    def "getInsights - should not flag and should not crash when category has no spending history"() {
        given: "Supermercado solo aparece este mes, nunca en los 6 meses anteriores"
        monthlySummaryService.getSummary(workspaceId, 2025, 6) >>
                summaryWith(2025, 6, "ARS", [new CategoryAmountRecord("Supermercado", new BigDecimal("500.00"))])
        (1..6).each { monthsAgo ->
            def ym = java.time.YearMonth.of(2025, 6).minusMonths(monthsAgo)
            monthlySummaryService.getSummary(workspaceId, ym.year, ym.monthValue) >>
                    summaryWith(ym.year, ym.monthValue, "ARS", [])
        }

        when:
        def result = service.getInsights(workspaceId)

        then:
        noExceptionThrown()
        result.isEmpty()
    }

    def "getInsights - should flag category well above its 6-month average"() {
        given:
        monthlySummaryService.getSummary(workspaceId, 2025, 6) >>
                summaryWith(2025, 6, "ARS", [new CategoryAmountRecord("Supermercado", new BigDecimal("2000.00"))])
        (1..6).each { monthsAgo ->
            def ym = java.time.YearMonth.of(2025, 6).minusMonths(monthsAgo)
            monthlySummaryService.getSummary(workspaceId, ym.year, ym.monthValue) >>
                    summaryWith(ym.year, ym.monthValue, "ARS",
                            [new CategoryAmountRecord("Supermercado", new BigDecimal("1000.00"))])
        }

        when:
        def result = service.getInsights(workspaceId)

        then:
        result.size() == 1
        result[0].category() == "Supermercado"
        result[0].currency() == "ARS"
        result[0].currentAmount() == new BigDecimal("2000.00")
        result[0].averageAmount() == new BigDecimal("1000.00")
        result[0].percentDeviation() == new BigDecimal("100.00")
        result[0].direction() == InsightDirection.ABOVE
    }

    def "getInsights - should flag category well below its 6-month average"() {
        given:
        monthlySummaryService.getSummary(workspaceId, 2025, 6) >>
                summaryWith(2025, 6, "ARS", [new CategoryAmountRecord("Ocio", new BigDecimal("400.00"))])
        (1..6).each { monthsAgo ->
            def ym = java.time.YearMonth.of(2025, 6).minusMonths(monthsAgo)
            monthlySummaryService.getSummary(workspaceId, ym.year, ym.monthValue) >>
                    summaryWith(ym.year, ym.monthValue, "ARS",
                            [new CategoryAmountRecord("Ocio", new BigDecimal("1000.00"))])
        }

        when:
        def result = service.getInsights(workspaceId)

        then:
        result.size() == 1
        result[0].category() == "Ocio"
        result[0].percentDeviation() == new BigDecimal("60.00")
        result[0].direction() == InsightDirection.BELOW
    }

    def "getInsights - should not flag category within the normal deviation range"() {
        given: "10% de desviación, por debajo del umbral de 25%"
        monthlySummaryService.getSummary(workspaceId, 2025, 6) >>
                summaryWith(2025, 6, "ARS", [new CategoryAmountRecord("Transporte", new BigDecimal("1100.00"))])
        (1..6).each { monthsAgo ->
            def ym = java.time.YearMonth.of(2025, 6).minusMonths(monthsAgo)
            monthlySummaryService.getSummary(workspaceId, ym.year, ym.monthValue) >>
                    summaryWith(ym.year, ym.monthValue, "ARS",
                            [new CategoryAmountRecord("Transporte", new BigDecimal("1000.00"))])
        }

        when:
        def result = service.getInsights(workspaceId)

        then:
        result.isEmpty()
    }

    def "evaluateCategory - should flag using the given amount instead of the actual monthly total"() {
        given: "el mes actual no tiene ningún gasto en Supermercado todavía, pero se evalúa un monto puntual"
        (1..6).each { monthsAgo ->
            def ym = java.time.YearMonth.of(2025, 6).minusMonths(monthsAgo)
            monthlySummaryService.getSummary(workspaceId, ym.year, ym.monthValue) >>
                    summaryWith(ym.year, ym.monthValue, "ARS",
                            [new CategoryAmountRecord("Supermercado", new BigDecimal("1000.00"))])
        }

        when:
        def result = service.evaluateCategory(workspaceId, "Supermercado", "ARS", new BigDecimal("2000.00"))

        then:
        0 * workspaceQueryService.verifyUserIsMemberOfWorkspace(_ as Long, _ as Long)
        0 * monthlySummaryService.getSummary(workspaceId, 2025, 6)
        result.isPresent()
        result.get().percentDeviation() == new BigDecimal("100.00")
        result.get().direction() == InsightDirection.ABOVE
    }

    def "evaluateCategory - should not flag when the given amount stays within the normal range"() {
        given:
        (1..6).each { monthsAgo ->
            def ym = java.time.YearMonth.of(2025, 6).minusMonths(monthsAgo)
            monthlySummaryService.getSummary(workspaceId, ym.year, ym.monthValue) >>
                    summaryWith(ym.year, ym.monthValue, "ARS",
                            [new CategoryAmountRecord("Supermercado", new BigDecimal("1000.00"))])
        }

        when:
        def result = service.evaluateCategory(workspaceId, "Supermercado", "ARS", new BigDecimal("1100.00"))

        then:
        result.isEmpty()
    }
}
