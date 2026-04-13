package api.m2.movements.unit.aspect

import api.m2.movements.aspect.membership.MovementWorkspaceResolver
import api.m2.movements.entities.Movement
import api.m2.movements.entities.Workspace
import api.m2.movements.enums.MembershipDomain
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.repositories.MovementRepository
import spock.lang.Specification

class MovementWorkspaceResolverTest extends Specification {

    MovementRepository movementRepository = Mock(MovementRepository)
    MovementWorkspaceResolver resolver

    def setup() {
        resolver = new MovementWorkspaceResolver(movementRepository)
    }

    def "supports - should return true for MOVEMENT domain"() {
        expect:
        resolver.supports(MembershipDomain.MOVEMENT) == true
    }

    def "supports - should return false for other domains"() {
        expect:
        resolver.supports(MembershipDomain.INCOME) == false
        resolver.supports(MembershipDomain.SUBSCRIPTION) == false
        resolver.supports(MembershipDomain.BUDGET) == false
    }

    def "resolveWorkspaceId - should return workspace id when movement exists"() {
        given:
        def workspace = Stub(Workspace) { getId() >> 42L }
        def movement = Stub(Movement) { getWorkspace() >> workspace }
        movementRepository.findById(10L) >> Optional.of(movement)

        when:
        def result = resolver.resolveWorkspaceId(10L)

        then:
        result == 42L
    }

    def "resolveWorkspaceId - should throw EntityNotFoundException when movement does not exist"() {
        given:
        movementRepository.findById(999L) >> Optional.empty()

        when:
        resolver.resolveWorkspaceId(999L)

        then:
        def ex = thrown(EntityNotFoundException)
        ex.message.contains("999")
    }
}
