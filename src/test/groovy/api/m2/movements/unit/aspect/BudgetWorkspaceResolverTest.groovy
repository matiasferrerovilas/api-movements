package api.m2.movements.unit.aspect

import api.m2.movements.aspect.membership.BudgetWorkspaceResolver
import api.m2.movements.entities.Budget
import api.m2.movements.entities.Workspace
import api.m2.movements.enums.MembershipDomain
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.repositories.BudgetRepository
import spock.lang.Specification

class BudgetWorkspaceResolverTest extends Specification {

    BudgetRepository budgetRepository = Mock(BudgetRepository)
    BudgetWorkspaceResolver resolver

    def setup() {
        resolver = new BudgetWorkspaceResolver(budgetRepository)
    }

    def "supports - should return true for BUDGET domain"() {
        expect:
        resolver.supports(MembershipDomain.BUDGET) == true
    }

    def "supports - should return false for other domains"() {
        expect:
        resolver.supports(MembershipDomain.MOVEMENT) == false
        resolver.supports(MembershipDomain.INCOME) == false
        resolver.supports(MembershipDomain.SUBSCRIPTION) == false
    }

    def "resolveWorkspaceId - should return workspace id when budget exists"() {
        given:
        def workspace = Stub(Workspace) { getId() >> 55L }
        def budget = Stub(Budget) { getWorkspace() >> workspace }
        budgetRepository.findById(40L) >> Optional.of(budget)

        when:
        def result = resolver.resolveWorkspaceId(40L)

        then:
        result == 55L
    }

    def "resolveWorkspaceId - should throw EntityNotFoundException when budget does not exist"() {
        given:
        budgetRepository.findById(666L) >> Optional.empty()

        when:
        resolver.resolveWorkspaceId(666L)

        then:
        def ex = thrown(EntityNotFoundException)
        ex.message.contains("666")
    }
}
