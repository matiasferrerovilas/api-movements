package api.m2.movements.unit.services

import api.m2.movements.clients.identity.response.UserMe
import api.m2.movements.entities.Goal
import api.m2.movements.entities.commons.Currency
import api.m2.movements.mappers.GoalMapper
import api.m2.movements.repositories.GoalRepository
import api.m2.movements.services.goals.GoalQueryService
import api.m2.movements.services.user.UserService
import api.m2.movements.services.workspaces.WorkspaceQueryService
import org.mapstruct.factory.Mappers
import spock.lang.Specification

class GoalQueryServiceTest extends Specification {

    GoalMapper goalMapper = Mappers.getMapper(GoalMapper)
    GoalRepository goalRepository = Mock(GoalRepository)
    WorkspaceQueryService workspaceQueryService = Mock(WorkspaceQueryService)
    UserService userService = Mock(UserService)

    GoalQueryService service

    def setup() {
        service = new GoalQueryService(goalRepository, goalMapper, workspaceQueryService, userService)
    }

    def "getByWorkspace - should verify membership and return mapped goals with progressPercent"() {
        given:
        def workspaceId = 5L
        def currency = Stub(Currency) { getSymbol() >> "ARS"; getId() >> 1L }
        def goal = new Goal(id: 1L, workspaceId: workspaceId, name: "Auto nuevo", currency: currency,
                targetAmount: new BigDecimal("10000.00"), currentAmount: new BigDecimal("2500.00"))

        userService.getMe() >> new UserMe(9L, "a@b.com", "A", "B", "PERSONAL", null)
        goalRepository.findByWorkspaceId(workspaceId) >> [goal]

        when:
        def result = service.getByWorkspace(workspaceId)

        then:
        1 * workspaceQueryService.verifyUserIsMemberOfWorkspace(workspaceId, 9L)
        result.size() == 1
        result[0].name() == "Auto nuevo"
        result[0].progressPercent() == new BigDecimal("25.00")
    }

    def "getByWorkspace - should cap progressPercent at 100 when currentAmount exceeds target"() {
        given:
        def workspaceId = 5L
        def currency = Stub(Currency) { getSymbol() >> "ARS"; getId() >> 1L }
        def goal = new Goal(id: 2L, workspaceId: workspaceId, name: "Meta superada", currency: currency,
                targetAmount: new BigDecimal("1000.00"), currentAmount: new BigDecimal("1500.00"))

        userService.getMe() >> new UserMe(9L, "a@b.com", "A", "B", "PERSONAL", null)
        goalRepository.findByWorkspaceId(workspaceId) >> [goal]

        when:
        def result = service.getByWorkspace(workspaceId)

        then:
        result[0].progressPercent() == new BigDecimal("100.00")
    }

    def "getByWorkspace - should return empty list when workspace has no goals"() {
        given:
        def workspaceId = 5L
        userService.getMe() >> new UserMe(9L, "a@b.com", "A", "B", "PERSONAL", null)
        goalRepository.findByWorkspaceId(workspaceId) >> []

        when:
        def result = service.getByWorkspace(workspaceId)

        then:
        result.isEmpty()
    }
}
