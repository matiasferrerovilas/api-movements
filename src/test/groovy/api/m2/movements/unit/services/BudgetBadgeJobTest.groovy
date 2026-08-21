package api.m2.movements.unit.services

import api.m2.movements.repositories.MovementRepository
import api.m2.movements.services.gamification.BudgetBadgeJob
import api.m2.movements.services.gamification.BudgetBadgeService
import spock.lang.Specification

class BudgetBadgeJobTest extends Specification {

    MovementRepository movementRepository = Mock()
    BudgetBadgeService budgetBadgeService = Mock()

    BudgetBadgeJob job

    def setup() {
        job = new BudgetBadgeJob(movementRepository, budgetBadgeService)
    }

    def "evaluateClosedBudgets - does nothing when there are no workspaces"() {
        given:
        movementRepository.findDistinctWorkspaceIds() >> []

        when:
        job.evaluateClosedBudgets()

        then:
        0 * budgetBadgeService.evaluateClosedPeriod(*_)
    }

    def "evaluateClosedBudgets - evaluates each workspace exactly once, for the same year and month"() {
        given:
        def workspaceIds = [1L, 2L, 3L]
        movementRepository.findDistinctWorkspaceIds() >> workspaceIds
        int capturedYear = 0
        int capturedMonth = 0

        when:
        job.evaluateClosedBudgets()

        then:
        workspaceIds.each { id ->
            1 * budgetBadgeService.evaluateClosedPeriod(id, _ as Integer, _ as Integer) >> { Long wid, Integer y, Integer m ->
                capturedYear = y
                capturedMonth = m
            }
        }
    }
}
