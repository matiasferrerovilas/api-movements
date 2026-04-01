package api.m2.movements.unit.services

import api.m2.movements.entities.Account
import api.m2.movements.entities.Movement
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.mappers.MovementMapper
import api.m2.movements.records.movements.ExpenseToUpdate
import api.m2.movements.records.movements.MovementDeletedEvent
import api.m2.movements.repositories.MovementRepository
import api.m2.movements.services.movements.MovementAddService
import api.m2.movements.services.movements.MovementFactory
import api.m2.movements.services.publishing.websockets.MovementPublishServiceWebSocket
import spock.lang.Specification

import java.util.Optional

class MovementAddServiceTest extends Specification {

    MovementRepository movementRepository = Mock(MovementRepository)
    MovementMapper movementMapper = Mock(MovementMapper)
    MovementFactory movementFactory = Mock(MovementFactory)
    MovementPublishServiceWebSocket movementPublishService = Mock(MovementPublishServiceWebSocket)

    MovementAddService service

    def setup() {
        service = new MovementAddService(
                movementRepository,
                movementMapper,
                movementFactory,
                movementPublishService
        )
    }

    def buildMovement(Long accountId) {
        def account = Stub(Account) { getId() >> accountId }
        def movement = Stub(Movement) { getAccount() >> account }
        return movement
    }

    // --- updateMovement ---
    // Note: membership check is handled by MembershipCheckAspect, not the service directly.

    def "updateMovement - should update movement when called"() {
        given:
        def dto = new ExpenseToUpdate(null, null, null, null, null, null, null, null)
        def movement = buildMovement(1L)
        movementRepository.findById(10L) >> Optional.of(movement)

        when:
        service.updateMovement(dto, 10L)

        then:
        1 * movementRepository.save(_ as Movement)
    }

    def "updateMovement - should throw EntityNotFoundException when movement does not exist"() {
        given:
        def dto = new ExpenseToUpdate(null, null, null, null, null, null, null, null)
        movementRepository.findById(999L) >> Optional.empty()

        when:
        service.updateMovement(dto, 999L)

        then:
        thrown(EntityNotFoundException)
        0 * movementRepository.save(_ as Movement)
    }

    // --- deleteMovement ---

    def "deleteMovement - should delete and publish event when called"() {
        given:
        def movement = buildMovement(2L)
        movementRepository.findById(20L) >> Optional.of(movement)

        when:
        service.deleteMovement(20L)

        then:
        1 * movementRepository.deleteById(20L)
        1 * movementPublishService.publishDeleteOfMovement(_ as MovementDeletedEvent)
    }

    def "deleteMovement - should throw EntityNotFoundException when movement does not exist"() {
        given:
        movementRepository.findById(999L) >> Optional.empty()

        when:
        service.deleteMovement(999L)

        then:
        thrown(EntityNotFoundException)
        0 * movementRepository.deleteById(_ as Long)
    }
}
