package api.m2.movements.unit.services

import api.m2.movements.entities.MonthlySummarySeen
import api.m2.movements.repositories.MonthlySummarySeenRepository
import api.m2.movements.repositories.MovementRepository
import api.m2.movements.services.balance.MonthCloseService
import api.m2.movements.services.workspaces.WorkspaceQueryService
import spock.lang.Specification

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class MonthCloseServiceTest extends Specification {

    MonthlySummarySeenRepository seenRepository = Mock()
    MovementRepository movementRepository = Mock()
    WorkspaceQueryService workspaceQueryService = Mock()

    // 10 de septiembre de 2026 — el mes cerrado es agosto/2026.
    Clock clock = Clock.fixed(Instant.parse("2026-09-10T09:00:00Z"), ZoneOffset.UTC)

    MonthCloseService service

    Long workspaceId = 7L
    Long userId = 42L

    def setup() {
        service = new MonthCloseService(seenRepository, movementRepository, workspaceQueryService, clock)
    }

    def "pendingFor - returns the previous month when it was not seen and the workspace has movements"() {
        given:
        seenRepository.existsByWorkspaceIdAndUserIdAndYearAndMonth(workspaceId, userId, 2026, 8) >> false
        movementRepository.existsByWorkspaceId(workspaceId) >> true

        when:
        def result = service.pendingFor(workspaceId, userId)

        then:
        result.isPresent()
        result.get().year() == 2026
        result.get().month() == 8
    }

    def "pendingFor - returns empty when the user already dismissed that month"() {
        given:
        seenRepository.existsByWorkspaceIdAndUserIdAndYearAndMonth(workspaceId, userId, 2026, 8) >> true

        when:
        def result = service.pendingFor(workspaceId, userId)

        then:
        result.isEmpty()
        0 * movementRepository.existsByWorkspaceId(_)
    }

    def "pendingFor - returns empty for a brand-new workspace with no movements"() {
        given:
        seenRepository.existsByWorkspaceIdAndUserIdAndYearAndMonth(workspaceId, userId, 2026, 8) >> false
        movementRepository.existsByWorkspaceId(workspaceId) >> false

        when:
        def result = service.pendingFor(workspaceId, userId)

        then:
        result.isEmpty()
    }

    def "markSeen - verifies membership and persists the seen row when missing"() {
        given:
        seenRepository.existsByWorkspaceIdAndUserIdAndYearAndMonth(workspaceId, userId, 2026, 8) >> false

        when:
        service.markSeen(workspaceId, userId, 2026, 8)

        then:
        1 * workspaceQueryService.verifyUserIsMemberOfWorkspace(workspaceId, userId)
        1 * seenRepository.save({ MonthlySummarySeen it ->
            it.workspaceId == workspaceId && it.userId == userId && it.year == 2026 && it.month == 8
        })
    }

    def "markSeen - is idempotent when the row already exists"() {
        given:
        seenRepository.existsByWorkspaceIdAndUserIdAndYearAndMonth(workspaceId, userId, 2026, 8) >> true

        when:
        service.markSeen(workspaceId, userId, 2026, 8)

        then:
        1 * workspaceQueryService.verifyUserIsMemberOfWorkspace(workspaceId, userId)
        0 * seenRepository.save(_)
    }
}
