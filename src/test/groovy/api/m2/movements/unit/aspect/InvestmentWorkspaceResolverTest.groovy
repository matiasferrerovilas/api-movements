package api.m2.movements.unit.aspect

import api.m2.movements.movements.enums.MembershipDomain
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.investment.aspect.InvestmentWorkspaceResolver
import api.m2.movements.investment.entities.Investment
import api.m2.movements.investment.repositories.InvestmentRepository
import spock.lang.Specification

class InvestmentWorkspaceResolverTest extends Specification {

    InvestmentRepository investmentRepository = Mock(InvestmentRepository)
    InvestmentWorkspaceResolver resolver

    def setup() {
        resolver = new InvestmentWorkspaceResolver(investmentRepository)
    }

    def "supports - should return true for INVESTMENT domain"() {
        expect:
        resolver.supports(MembershipDomain.INVESTMENT) == true
    }

    def "supports - should return false for other domains"() {
        expect:
        resolver.supports(MembershipDomain.MOVEMENT) == false
        resolver.supports(MembershipDomain.INCOME) == false
        resolver.supports(MembershipDomain.SUBSCRIPTION) == false
        resolver.supports(MembershipDomain.BUDGET) == false
    }

    def "resolveWorkspaceId - should return workspace id when investment exists"() {
        given:
        def investment = Stub(Investment) { getWorkspaceId() >> 42L }
        investmentRepository.findById(10L) >> Optional.of(investment)

        when:
        def result = resolver.resolveWorkspaceId(10L)

        then:
        result == 42L
    }

    def "resolveWorkspaceId - should throw EntityNotFoundException when investment does not exist"() {
        given:
        investmentRepository.findById(999L) >> Optional.empty()

        when:
        resolver.resolveWorkspaceId(999L)

        then:
        def ex = thrown(EntityNotFoundException)
        ex.message.contains("999")
    }
}
