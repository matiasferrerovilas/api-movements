package api.m2.movements.unit.services

import api.m2.movements.entities.Badge
import api.m2.movements.entities.Budget
import api.m2.movements.entities.commons.Category
import api.m2.movements.entities.commons.Currency
import api.m2.movements.enums.BadgeType
import api.m2.movements.enums.NotificationSeverity
import api.m2.movements.repositories.BadgeRepository
import api.m2.movements.repositories.BudgetRepository
import api.m2.movements.services.gamification.BudgetBadgeService
import api.m2.movements.services.notifications.NotificationService
import api.m2.movements.services.workspaces.WorkspaceContextService
import spock.lang.Specification

class BudgetBadgeServiceTest extends Specification {

    BudgetRepository budgetRepository = Mock()
    BadgeRepository badgeRepository = Mock()
    NotificationService notificationService = Mock()
    WorkspaceContextService workspaceContextService = Mock()

    BudgetBadgeService service

    def setup() {
        service = new BudgetBadgeService(budgetRepository, badgeRepository, notificationService, workspaceContextService)
    }

    def buildBudget(BigDecimal amount, Integer year = null, Integer month = null, Long categoryId = 5L) {
        def category = categoryId == null ? null : Stub(Category) { getId() >> categoryId; getDescription() >> "Comida" }
        def currency = Stub(Currency) { getSymbol() >> "ARS" }
        return Stub(Budget) {
            getWorkspaceId() >> 1L
            getCategory() >> category
            getCurrency() >> currency
            getAmount() >> amount
            getYear() >> year
            getMonth() >> month
        }
    }

    def "evaluateClosedPeriod - awards a badge and notifies when spending stayed within the limit"() {
        given:
        def budget = buildBudget(new BigDecimal("40000"))
        budgetRepository.findByWorkspaceAndPeriod(1L, 2026, 8) >> [budget]
        badgeRepository.existsByWorkspaceIdAndCategoryIdAndYearAndMonthAndType(1L, 5L, 2026, 8, BadgeType.BUDGET_MET) >> false
        budgetRepository.sumSpentByCategoryAndPeriod(1L, "Comida", "ARS", 2026, 8) >> new BigDecimal("30000")

        when:
        service.evaluateClosedPeriod(1L, 2026, 8)

        then:
        1 * badgeRepository.save({ Badge b -> b.workspaceId == 1L && b.type == BadgeType.BUDGET_MET && b.year == 2026 && b.month == 8 })
        1 * notificationService.publish(1L, "¡Presupuesto cumplido!", _ as String, NotificationSeverity.SUCCESS)
    }

    def "evaluateClosedPeriod - does not award a badge when spending exceeded the limit"() {
        given:
        def budget = buildBudget(new BigDecimal("40000"))
        budgetRepository.findByWorkspaceAndPeriod(1L, 2026, 8) >> [budget]
        badgeRepository.existsByWorkspaceIdAndCategoryIdAndYearAndMonthAndType(1L, 5L, 2026, 8, BadgeType.BUDGET_MET) >> false
        budgetRepository.sumSpentByCategoryAndPeriod(1L, "Comida", "ARS", 2026, 8) >> new BigDecimal("45000")

        when:
        service.evaluateClosedPeriod(1L, 2026, 8)

        then:
        0 * badgeRepository.save(_)
        0 * notificationService.publish(*_)
    }

    def "evaluateClosedPeriod - does not award a duplicate badge for the same period"() {
        given:
        def budget = buildBudget(new BigDecimal("40000"))
        budgetRepository.findByWorkspaceAndPeriod(1L, 2026, 8) >> [budget]
        badgeRepository.existsByWorkspaceIdAndCategoryIdAndYearAndMonthAndType(1L, 5L, 2026, 8, BadgeType.BUDGET_MET) >> true

        when:
        service.evaluateClosedPeriod(1L, 2026, 8)

        then:
        0 * budgetRepository.sumSpentByCategoryAndPeriod(*_)
        0 * badgeRepository.save(_)
    }

    def "evaluateClosedPeriod - skips budgets without a category"() {
        given:
        def budget = buildBudget(new BigDecimal("40000"), null, null, null)
        budgetRepository.findByWorkspaceAndPeriod(1L, 2026, 8) >> [budget]

        when:
        service.evaluateClosedPeriod(1L, 2026, 8)

        then:
        0 * badgeRepository.existsByWorkspaceIdAndCategoryIdAndYearAndMonthAndType(*_)
        0 * badgeRepository.save(_)
    }

    def "evaluateClosedPeriod - skips yearly running-total budgets to avoid a badge every month"() {
        given:
        def budget = buildBudget(new BigDecimal("100000"), 2026, null)
        budgetRepository.findByWorkspaceAndPeriod(1L, 2026, 8) >> [budget]

        when:
        service.evaluateClosedPeriod(1L, 2026, 8)

        then:
        0 * badgeRepository.existsByWorkspaceIdAndCategoryIdAndYearAndMonthAndType(*_)
        0 * badgeRepository.save(_)
    }

    def "evaluateClosedPeriod - evaluates an always-active budget (year null, month null) against that month's spend"() {
        given:
        def budget = buildBudget(new BigDecimal("40000"), null, null)
        budgetRepository.findByWorkspaceAndPeriod(1L, 2026, 8) >> [budget]
        badgeRepository.existsByWorkspaceIdAndCategoryIdAndYearAndMonthAndType(1L, 5L, 2026, 8, BadgeType.BUDGET_MET) >> false
        budgetRepository.sumSpentByCategoryAndPeriod(1L, "Comida", "ARS", 2026, 8) >> new BigDecimal("10000")

        when:
        service.evaluateClosedPeriod(1L, 2026, 8)

        then:
        1 * badgeRepository.save(_)
    }

    def "getBadges - lists badges for the active workspace, most recent first"() {
        given:
        def category = Stub(Category) { getDescription() >> "Comida" }
        def badge = Stub(Badge) {
            getId() >> 9L
            getCategory() >> category
            getType() >> BadgeType.BUDGET_MET
            getYear() >> 2026
            getMonth() >> 8
            getEarnedAt() >> null
        }
        workspaceContextService.getActiveWorkspaceId() >> 1L
        badgeRepository.findByWorkspaceIdOrderByEarnedAtDesc(1L) >> [badge]

        when:
        def result = service.getBadges()

        then:
        result.size() == 1
        result[0].categoryDescription() == "Comida"
        result[0].type() == BadgeType.BUDGET_MET
    }
}
