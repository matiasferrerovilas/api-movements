package api.m2.movements.unit.services

import api.m2.movements.entities.Category
import api.m2.movements.entities.Currency
import api.m2.movements.entities.Movement
import api.m2.movements.entities.User
import api.m2.movements.entities.Workspace
import api.m2.movements.enums.MovementType
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.exceptions.PermissionDeniedException
import api.m2.movements.mappers.CategoryMapper
import api.m2.movements.mappers.CurrencyMapper
import api.m2.movements.mappers.MovementMapper
import api.m2.movements.mappers.MovementMapperImpl
import api.m2.movements.mappers.UserMapper
import api.m2.movements.records.movements.ExpenseToUpdate
import api.m2.movements.records.movements.MovementDeletedEvent
import api.m2.movements.records.movements.MovementToAdd
import api.m2.movements.repositories.MovementRepository
import api.m2.movements.services.workspaces.WorkspaceQueryService
import api.m2.movements.services.movements.MovementAddService
import api.m2.movements.services.movements.MovementFactory
import api.m2.movements.services.publishing.websockets.MovementPublishServiceWebSocket
import api.m2.movements.services.user.UserService
import org.mapstruct.factory.Mappers
import org.springframework.test.util.ReflectionTestUtils
import spock.lang.Specification

import java.time.LocalDate

class MovementAddServiceTest extends Specification {

    MovementRepository movementRepository = Mock(MovementRepository)
    MovementMapper movementMapper
    MovementFactory movementFactory = Mock(MovementFactory)
    MovementPublishServiceWebSocket movementPublishService = Mock(MovementPublishServiceWebSocket)
    UserService userService = Mock(UserService)
    WorkspaceQueryService workspaceQueryService = Mock(WorkspaceQueryService)

    MovementAddService service

    def setup() {
        movementMapper = new MovementMapperImpl()
        ReflectionTestUtils.setField(movementMapper, "categoryMapper", Mappers.getMapper(CategoryMapper))
        ReflectionTestUtils.setField(movementMapper, "currencyMapper", Mappers.getMapper(CurrencyMapper))
        ReflectionTestUtils.setField(movementMapper, "userMapper", Mappers.getMapper(UserMapper))

        service = new MovementAddService(
                movementRepository,
                movementMapper,
                movementFactory,
                movementPublishService,
                userService,
                workspaceQueryService
        )
    }

    def buildMovement(Long workspaceId) {
        def workspace = Workspace.builder().id(workspaceId).name("Test Workspace").build()
        def movement = Movement.builder()
                .id(42L)
                .amount(new BigDecimal("500.00"))
                .description("Supermercado")
                .date(LocalDate.now())
                .type(MovementType.DEBITO)
                .workspace(workspace)
                .category(Category.builder().description("HOGAR").build())
                .currency(Currency.builder().id(1L).symbol("ARS").build())
                .owner(User.builder().id(10L).email("test@test.com").build())
                .build()
        return movement
    }

    // --- saveMovement ---

    def "saveMovement - should verify membership and save movement"() {
        given:
        def dto = new MovementToAdd(
                new BigDecimal("500.00"), LocalDate.now(), "Supermercado",
                "HOGAR", "GASTO", "ARS", null, null, null, 1L
        )
        def user = Stub(User) { getId() >> 10L }
        def movement = buildMovement(1L)

        userService.getAuthenticatedUser() >> user
        movementFactory.create(_ as MovementToAdd) >> movement

        when:
        service.saveMovement(dto)

        then:
        1 * workspaceQueryService.verifyUserIsMemberOfWorkspace(1L, 10L)
        1 * movementRepository.save(_ as Movement) >> movement
    }

    def "saveMovement - should throw PermissionDeniedException when user is not a member of the workspace"() {
        given:
        def dto = new MovementToAdd(
                new BigDecimal("500.00"), LocalDate.now(), "Supermercado",
                "HOGAR", "GASTO", "ARS", null, null, null, 99L
        )
        def user = Stub(User) { getId() >> 10L }

        userService.getAuthenticatedUser() >> user
        workspaceQueryService.verifyUserIsMemberOfWorkspace(99L, 10L) >> {
            throw new PermissionDeniedException("No tienes permiso para operar sobre este recurso")
        }

        when:
        service.saveMovement(dto)

        then:
        thrown(PermissionDeniedException)
        0 * movementRepository.save(_ as Movement)
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

