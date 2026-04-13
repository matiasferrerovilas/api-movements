package api.m2.movements.unit.aspect

import api.m2.movements.aspect.membership.WorkspaceIdResolver
import api.m2.movements.aspect.membership.WorkspaceIdResolverRegistry
import api.m2.movements.enums.MembershipDomain
import spock.lang.Specification

class WorkspaceIdResolverRegistryTest extends Specification {

    def "resolve - should delegate to resolver that supports the domain"() {
        given:
        def movementResolver = Mock(WorkspaceIdResolver) {
            supports(MembershipDomain.MOVEMENT) >> true
            resolveWorkspaceId(10L) >> 100L
        }
        def incomeResolver = Mock(WorkspaceIdResolver) {
            supports(MembershipDomain.MOVEMENT) >> false
        }
        def registry = new WorkspaceIdResolverRegistry([movementResolver, incomeResolver])

        when:
        def result = registry.resolve(MembershipDomain.MOVEMENT, 10L)

        then:
        result == 100L
    }

    def "resolve - should throw IllegalArgumentException when no resolver supports domain"() {
        given:
        def resolver = Mock(WorkspaceIdResolver) {
            supports(_ as MembershipDomain) >> false
        }
        def registry = new WorkspaceIdResolverRegistry([resolver])

        when:
        registry.resolve(MembershipDomain.BUDGET, 1L)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("BUDGET")
    }

    def "resolve - should work with empty resolver list"() {
        given:
        def registry = new WorkspaceIdResolverRegistry([])

        when:
        registry.resolve(MembershipDomain.MOVEMENT, 1L)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("MOVEMENT")
    }

    def "resolve - should use first matching resolver when multiple support same domain"() {
        given:
        def firstResolver = Mock(WorkspaceIdResolver) {
            supports(MembershipDomain.INCOME) >> true
            resolveWorkspaceId(5L) >> 50L
        }
        def secondResolver = Mock(WorkspaceIdResolver) {
            supports(MembershipDomain.INCOME) >> true
            resolveWorkspaceId(5L) >> 999L
        }
        def registry = new WorkspaceIdResolverRegistry([firstResolver, secondResolver])

        when:
        def result = registry.resolve(MembershipDomain.INCOME, 5L)

        then:
        result == 50L
    }
}
