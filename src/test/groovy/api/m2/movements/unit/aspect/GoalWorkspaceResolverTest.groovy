package api.m2.movements.unit.aspect

import api.m2.movements.aspect.membership.GoalWorkspaceResolver
import api.m2.movements.entities.Goal
import api.m2.movements.enums.MembershipDomain
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.repositories.GoalRepository
import spock.lang.Specification

class GoalWorkspaceResolverTest extends Specification {

    GoalRepository goalRepository = Mock(GoalRepository)
    GoalWorkspaceResolver resolver

    def setup() {
        resolver = new GoalWorkspaceResolver(goalRepository)
    }

    def "supports - should return true for GOAL domain"() {
        expect:
        resolver.supports(MembershipDomain.GOAL) == true
    }

    def "supports - should return false for other domains"() {
        expect:
        resolver.supports(MembershipDomain.MOVEMENT) == false
        resolver.supports(MembershipDomain.INCOME) == false
        resolver.supports(MembershipDomain.SUBSCRIPTION) == false
        resolver.supports(MembershipDomain.BUDGET) == false
    }

    def "resolveWorkspaceId - should return workspace id when goal exists"() {
        given:
        def goal = Stub(Goal) { getWorkspaceId() >> 77L }
        goalRepository.findById(30L) >> Optional.of(goal)

        when:
        def result = resolver.resolveWorkspaceId(30L)

        then:
        result == 77L
    }

    def "resolveWorkspaceId - should throw EntityNotFoundException when goal does not exist"() {
        given:
        goalRepository.findById(999L) >> Optional.empty()

        when:
        resolver.resolveWorkspaceId(999L)

        then:
        def ex = thrown(EntityNotFoundException)
        ex.message.contains("999")
    }
}
