package api.m2.movements.unit.aspect

import api.m2.movements.aspect.membership.SubscriptionWorkspaceResolver
import api.m2.movements.entities.Subscription
import api.m2.movements.entities.Workspace
import api.m2.movements.enums.MembershipDomain
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.repositories.SubscriptionRepository
import spock.lang.Specification

class SubscriptionWorkspaceResolverTest extends Specification {

    SubscriptionRepository subscriptionRepository = Mock(SubscriptionRepository)
    SubscriptionWorkspaceResolver resolver

    def setup() {
        resolver = new SubscriptionWorkspaceResolver(subscriptionRepository)
    }

    def "supports - should return true for SUBSCRIPTION domain"() {
        expect:
        resolver.supports(MembershipDomain.SUBSCRIPTION) == true
    }

    def "supports - should return false for other domains"() {
        expect:
        resolver.supports(MembershipDomain.MOVEMENT) == false
        resolver.supports(MembershipDomain.INCOME) == false
        resolver.supports(MembershipDomain.BUDGET) == false
    }

    def "resolveWorkspaceId - should return workspace id when subscription exists"() {
        given:
        def workspace = Stub(Workspace) { getId() >> 33L }
        def subscription = Stub(Subscription) { getWorkspace() >> workspace }
        subscriptionRepository.findById(30L) >> Optional.of(subscription)

        when:
        def result = resolver.resolveWorkspaceId(30L)

        then:
        result == 33L
    }

    def "resolveWorkspaceId - should throw EntityNotFoundException when subscription does not exist"() {
        given:
        subscriptionRepository.findById(777L) >> Optional.empty()

        when:
        resolver.resolveWorkspaceId(777L)

        then:
        def ex = thrown(EntityNotFoundException)
        ex.message.contains("777")
    }
}
