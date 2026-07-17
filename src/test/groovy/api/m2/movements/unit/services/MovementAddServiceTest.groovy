package api.m2.movements.unit.services

import api.m2.movements.entities.commons.Category
import api.m2.movements.entities.commons.Currency
import api.m2.movements.entities.movements.Movement

import api.m2.movements.enums.MovementType
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.exceptions.PermissionDeniedException
import api.m2.movements.mappers.CategoryMapper
import api.m2.movements.mappers.CurrencyMapper
import api.m2.movements.mappers.MovementMapper
import api.m2.movements.mappers.MovementMapperImpl
import api.m2.movements.records.movements.ExpenseToUpdate
import api.m2.movements.records.movements.MovementDeletedEvent
import api.m2.movements.records.movements.MovementRecord
import api.m2.movements.records.movements.MovementToAdd
import api.m2.movements.records.workspaces.WorkspaceBaseRecord
import api.m2.movements.repositories.MovementRepository
import api.m2.movements.services.movements.MovementAddService
import api.m2.movements.services.movements.MovementFactory
import api.m2.movements.services.user.UserService
import api.m2.movements.services.workspaces.WorkspaceQueryService
import org.mapstruct.factory.Mappers
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.util.ReflectionTestUtils
import spock.lang.Specification

import java.time.LocalDate

class MovementAddServiceTest extends Specification {

    MovementRepository movementRepository = Mock(MovementRepository)
    MovementMapper movementMapper
    MovementFactory movementFactory = Mock(MovementFactory)
    ApplicationEventPublisher eventPublisher = Mock(ApplicationEventPublisher)
    WorkspaceQueryService workspaceQueryService = Mock(WorkspaceQueryService)
    UserService userService = Mock(UserService)

    MovementAddService service

    def setup() {
        movementMapper = new MovementMapperImpl()
        ReflectionTestUtils.setField(movementMapper, "categoryMapper", Mappers.getMapper(CategoryMapper))
        ReflectionTestUtils.setField(movementMapper, "currencyMapper", Mappers.getMapper(CurrencyMapper))

        service = new MovementAddService(
                movementRepository,
                movementMapper,
                movementFactory,
                eventPublisher,
                workspaceQueryService,
                userService
        )
        workspaceQueryService.findWorkspaceNameById(_ as Long) >> "Familia"
        userService.getUserNamesByIds(_ as List<Long>) >> [:]
    }

    def buildMovement(Long workspaceId) {
        def movement = Movement.builder()
                .id(42L)
                .amount(new BigDecimal("500.00"))
                .description("Supermercado")
                .date(LocalDate.now())
                .type(MovementType.DEBITO)
                .workspaceId(workspaceId)
                .category(Category.builder().description("HOGAR").build())
                .currency(Currency.builder().id(1L).symbol("ARS").build())
                .ownerId(10L)
                .build()
        return movement
    }

    // --- saveMovement ---

    def "saveMovement - should verify membership and save movement"() {
        given:
        def dto = new MovementToAdd(
                new BigDecimal("500.00"), LocalDate.now(), "Supermercado",
                "HOGAR", "GASTO", "ARS", null, null, null
        )
        def movement = buildMovement(1L)

        movementFactory.create(_ as MovementToAdd) >> movement

        when:
        service.saveMovement(dto)

        then:
        1 * movementRepository.save(_ as Movement) >> movement
        1 * eventPublisher.publishEvent(_ as MovementRecord)
    }

    def "saveMovement - should publish MovementRecord enriched with workspace and owner metadata"() {
        given:
        def dto = new MovementToAdd(
                new BigDecimal("500.00"), LocalDate.now(), "Supermercado",
                "HOGAR", "GASTO", "ARS", null, null, null
        )
        def movement = buildMovement(1L)

        movementFactory.create(_ as MovementToAdd) >> movement
        movementRepository.save(movement) >> movement
        workspaceQueryService.findWorkspaceNameById(1L) >> "Familia"
        userService.getUserNamesByIds([10L]) >> [10L: "Matias"]

        when:
        def result = service.saveMovement(dto)

        then:
        result.metadata().workspace() == new WorkspaceBaseRecord(1L, "Familia")
        result.metadata().owner().givenName() == "Matias"
        result.metadata().owner().id() == 10L
    }

    def "saveMovement - should throw PermissionDeniedException when user is not a member of the workspace"() {
        given:
        def dto = new MovementToAdd(
                new BigDecimal("500.00"), LocalDate.now(), "Supermercado",
                "HOGAR", "GASTO", "ARS", null, null, null
        )

        movementFactory.create(_ as MovementToAdd) >> {
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
}

