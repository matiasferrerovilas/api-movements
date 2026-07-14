package api.m2.movements.unit.aspect

import api.m2.movements.aspect.membership.IncomeWorkspaceResolver
import api.m2.movements.movements.entities.movements.Income
import api.m2.movements.movements.enums.MembershipDomain
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.movements.repositories.IncomeRepository
import spock.lang.Specification

class IncomeWorkspaceResolverTest extends Specification {

    IncomeRepository incomeRepository = Mock(IncomeRepository)
    IncomeWorkspaceResolver resolver

    def setup() {
        resolver = new IncomeWorkspaceResolver(incomeRepository)
    }

    def "supports - should return true for INCOME domain"() {
        expect:
        resolver.supports(MembershipDomain.INCOME) == true
    }

    def "supports - should return false for other domains"() {
        expect:
        resolver.supports(MembershipDomain.MOVEMENT) == false
        resolver.supports(MembershipDomain.SUBSCRIPTION) == false
        resolver.supports(MembershipDomain.BUDGET) == false
    }

    def "resolveWorkspaceId - should return workspace id when income exists"() {
        given:
        def income = Stub(Income) { getWorkspaceId() >> 77L }
        incomeRepository.findById(20L) >> Optional.of(income)

        when:
        def result = resolver.resolveWorkspaceId(20L)

        then:
        result == 77L
    }

    def "resolveWorkspaceId - should throw EntityNotFoundException when income does not exist"() {
        given:
        incomeRepository.findById(888L) >> Optional.empty()

        when:
        resolver.resolveWorkspaceId(888L)

        then:
        def ex = thrown(EntityNotFoundException)
        ex.message.contains("888")
    }
}
