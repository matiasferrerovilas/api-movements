package api.m2.movements.unit.services

import api.m2.movements.clients.identity.response.UserMe
import api.m2.movements.entities.commons.Category
import api.m2.movements.entities.commons.Currency
import api.m2.movements.entities.movements.Movement

import api.m2.movements.enums.MovementType
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.events.MovementDeletedEvent
import api.m2.movements.repositories.MovementRepository
import api.m2.movements.services.movements.MovementItemService
import api.m2.movements.services.user.UserService
import org.springframework.context.ApplicationEventPublisher
import api.m2.movements.services.movements.MovementDeleteService
import spock.lang.Specification

import java.time.LocalDate

class MovementDeleteServiceTest extends Specification {

    MovementRepository movementRepository = Mock(MovementRepository)
    MovementItemService movementItemService = Mock(MovementItemService)
    ApplicationEventPublisher eventPublisher = Mock(ApplicationEventPublisher)
    UserService userService = Mock(UserService)

    MovementDeleteService service

    def setup() {
        service = new MovementDeleteService(
                movementRepository,
                movementItemService,
                eventPublisher,
                userService
        )
    }

    def buildMovement(Long workspaceId) {
        def movement = Movement.builder()
                .id(42L)
                .amount(new BigDecimal("500.00"))
                .description("Supermercado")
                .date(LocalDate.now())
                .type(MovementType.DEBITO)
                .workspaceId(workspaceId)
                .categories([Category.builder().description("HOGAR").build()] as Set)
                .currency(Currency.builder().id(1L).symbol("ARS").build())
                .ownerId(10L)
                .build()
        return movement
    }

    def "deleteMovement - should delete and publish event when called"() {
        given:
        def movement = buildMovement(2L)
        movementRepository.findById(20L) >> Optional.of(movement)

        when:
        service.deleteMovement(20L)

        then:
        1 * movementRepository.deleteById(20L)
        1 * eventPublisher.publishEvent(_ as MovementDeletedEvent)
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

    def "deleteAllMovementsOfCurrentUser - should delete only the caller's movements and publish one event each"() {
        given:
        userService.getMe() >> new UserMe(5L, null, null, null, null, null)
        def first = Movement.builder().id(1L).workspaceId(2L).ownerId(5L).build()
        def second = Movement.builder().id(2L).workspaceId(3L).ownerId(5L).build()
        movementRepository.findAllByOwnerId(5L) >> [first, second]

        when:
        def deleted = service.deleteAllMovementsOfCurrentUser()

        then:
        deleted == 2
        1 * movementItemService.deleteItemsOf([1L, 2L])
        1 * movementRepository.deleteAll([first, second])
        1 * eventPublisher.publishEvent(new MovementDeletedEvent(1L, 2L))
        1 * eventPublisher.publishEvent(new MovementDeletedEvent(2L, 3L))
    }

    def "deleteAllMovementsOfCurrentUser - should do nothing when the caller has no movements"() {
        given:
        userService.getMe() >> new UserMe(5L, null, null, null, null, null)
        movementRepository.findAllByOwnerId(5L) >> []

        when:
        def deleted = service.deleteAllMovementsOfCurrentUser()

        then:
        deleted == 0
        0 * movementRepository.deleteAll(_)
        0 * eventPublisher.publishEvent(_)
    }
}
