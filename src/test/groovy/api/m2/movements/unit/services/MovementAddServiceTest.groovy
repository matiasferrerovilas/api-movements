package api.m2.movements.unit.services

import api.m2.movements.clients.identity.response.UserMe
import api.m2.movements.entities.WorkspaceCategory
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
import api.m2.movements.events.CategoryCorrectedEvent
import api.m2.movements.records.categories.CategoryUpdateRecord
import api.m2.movements.records.movements.ExpenseToUpdate
import api.m2.movements.events.MovementDeletedEvent
import api.m2.movements.records.movements.MovementRecord
import api.m2.movements.records.movements.MovementToAdd
import api.m2.movements.records.workspaces.WorkspaceBaseRecord
import api.m2.movements.repositories.MovementRepository
import api.m2.movements.repositories.WorkspaceCategoryRepository
import api.m2.movements.records.movements.ImportResultRecord
import api.m2.movements.services.movements.MovementAddService
import api.m2.movements.services.movements.MovementDuplicateFilter
import api.m2.movements.services.movements.MovementFactory
import api.m2.movements.services.movements.MovementItemService
import api.m2.movements.services.movements.MovementPersister
import api.m2.movements.services.movements.MovementRecordAssembler
import api.m2.movements.services.user.UserService
import api.m2.movements.entities.commons.Bank
import api.m2.movements.services.workspaces.WorkspaceQueryService
import org.mapstruct.factory.Mappers
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.util.ReflectionTestUtils
import spock.lang.Specification

import java.time.LocalDate
import java.time.YearMonth

class MovementAddServiceTest extends Specification {

    MovementRepository movementRepository = Mock(MovementRepository)
    MovementMapper movementMapper
    MovementFactory movementFactory = Mock(MovementFactory)
    MovementItemService movementItemService = Mock(MovementItemService)
    ApplicationEventPublisher eventPublisher = Mock(ApplicationEventPublisher)
    WorkspaceQueryService workspaceQueryService = Mock(WorkspaceQueryService)
    UserService userService = Mock(UserService)
    MovementDuplicateFilter movementDuplicateFilter = Mock(MovementDuplicateFilter)
    WorkspaceCategoryRepository workspaceCategoryRepository = Mock(WorkspaceCategoryRepository)

    MovementAddService service

    def setup() {
        movementMapper = new MovementMapperImpl()
        ReflectionTestUtils.setField(movementMapper, "categoryMapper", Mappers.getMapper(CategoryMapper))
        ReflectionTestUtils.setField(movementMapper, "currencyMapper", Mappers.getMapper(CurrencyMapper))

        service = new MovementAddService(
                movementRepository,
                movementFactory,
                eventPublisher,
                movementDuplicateFilter,
                new MovementPersister(movementRepository, movementItemService, eventPublisher),
                new MovementRecordAssembler(movementMapper, Mappers.getMapper(CategoryMapper),
                        workspaceCategoryRepository, workspaceQueryService, userService)
        )
        workspaceCategoryRepository.findByWorkspaceIdAndIsActiveTrue(_ as Long) >> []
        workspaceQueryService.findWorkspaceNameById(_ as Long) >> "Familia"
        userService.getUserNamesByIds(_ as List<Long>) >> [:]
        movementItemService.getItems(_ as Long) >> []
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

    // Los iconos viven en WorkspaceCategory, no en Category: MovementMapper.toRecord los deja en
    // null, y el evento de websocket llegaba al front sin icono hasta el próximo refresh.
    def buildMovementWithCategory(Long workspaceId) {
        def movement = buildMovement(workspaceId)
        movement.categories = [Category.builder().id(7L).description("HOGAR").build()] as Set
        return movement
    }

    def workspaceCategoryFor(Movement movement) {
        return WorkspaceCategory.builder()
                .workspaceId(movement.workspaceId)
                .category(movement.categories.first())
                .iconName("ShoppingCartOutlined")
                .iconColor("#ff0000")
                .build()
    }

    // Note: membership check is handled by MembershipCheckAspect, not the service directly.

    // El type-change side effect vive en MovementMapper (updateMovement), no en MovementFactory
    // — se prueba acá directo contra el mapper real (no mockeado) que usa este test.

    def "saveMovement - should verify membership and save movement"() {
        given:
        def dto = new MovementToAdd(
                new BigDecimal("500.00"), LocalDate.now(), "Supermercado",
                ["HOGAR"], "GASTO", "ARS", null, null, null, null, null
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
                ["HOGAR"], "GASTO", "ARS", null, null, null, null, null
        )
        def movement = buildMovement(1L)

        movementFactory.create(_ as MovementToAdd) >> movement
        movementRepository.save(movement) >> movement
        workspaceQueryService.findWorkspaceNameById(1L) >> "Familia"

        when:
        def result = service.saveMovement(dto)

        then:
        1 * userService.getUserNamesByIds([10L]) >> [10L: "Matias"]
        result.metadata().workspace() == new WorkspaceBaseRecord(1L, "Familia")
        result.metadata().owner().givenName() == "Matias"
        result.metadata().owner().id() == 10L
    }

    def "saveMovement - should publish MovementRecord with the workspace's category icons"() {
        given:
        def dto = new MovementToAdd(
                new BigDecimal("500.00"), LocalDate.now(), "Supermercado",
                ["HOGAR"], "GASTO", "ARS", null, null, null, null, null
        )
        def movement = buildMovementWithCategory(1L)
        movementFactory.create(_ as MovementToAdd) >> movement
        movementRepository.save(movement) >> movement

        when:
        def result = service.saveMovement(dto)

        then:
        1 * workspaceCategoryRepository.findByWorkspaceIdAndIsActiveTrue(1L) >> [workspaceCategoryFor(movement)]
        1 * eventPublisher.publishEvent({
            it.categories().first().iconName() == "ShoppingCartOutlined" &&
                    it.categories().first().iconColor() == "#ff0000"
        })
        result.categories().first().iconName() == "ShoppingCartOutlined"
        result.categories().first().isActive()
    }

    def "saveMovement - should fall back to the default icon when the category has no workspace row"() {
        given:
        def dto = new MovementToAdd(
                new BigDecimal("500.00"), LocalDate.now(), "Supermercado",
                ["HOGAR"], "GASTO", "ARS", null, null, null, null, null
        )
        def movement = buildMovementWithCategory(1L)
        movementFactory.create(_ as MovementToAdd) >> movement
        movementRepository.save(movement) >> movement

        when:
        def result = service.saveMovement(dto)

        then:
        result.categories().first().iconName() == "QuestionOutlined"
        result.categories().first().iconColor() == "#d9d9d9"
    }

    def "saveMovement - should throw PermissionDeniedException when user is not a member of the workspace"() {
        given:
        def dto = new MovementToAdd(
                new BigDecimal("500.00"), LocalDate.now(), "Supermercado",
                ["HOGAR"], "GASTO", "ARS", null, null, null, null, null
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

    def "saveExpenseAll - should save only new movements and report duplicates"() {
        given:
        def dto = new MovementToAdd(
                new BigDecimal("500.00"), LocalDate.now(), "Supermercado",
                [], "DEBITO", "EUR", 0, 0, "SANTANDER", null, null
        )
        def existing = buildMovement(1L)
        def fresh = buildMovement(1L)
        movementFactory.create(_ as MovementToAdd) >>> [existing, fresh]
        movementDuplicateFilter.filterNew([existing, fresh], 1L) >> [fresh]

        when:
        def result = service.saveExpenseAll([dto, dto])

        then:
        1 * movementRepository.saveAll({ it.size() == 1 && it[0].is(fresh) }) >> [fresh]
        1 * eventPublisher.publishEvent(_ as MovementRecord)
        result == new ImportResultRecord(1, 1)
    }

    def "saveExpenseAll - should load the workspace icons once and publish every record with them"() {
        given:
        def dto = new MovementToAdd(
                new BigDecimal("500.00"), LocalDate.now(), "Supermercado",
                [], "DEBITO", "EUR", 0, 0, "SANTANDER", null, null
        )
        def first = buildMovementWithCategory(1L)
        def second = buildMovementWithCategory(1L)
        movementFactory.create(_ as MovementToAdd) >>> [first, second]
        movementDuplicateFilter.filterNew([first, second], 1L) >> [first, second]
        movementRepository.saveAll(_ as List<Movement>) >> [first, second]

        when:
        service.saveExpenseAll([dto, dto])

        then:
        1 * workspaceCategoryRepository.findByWorkspaceIdAndIsActiveTrue(1L) >> [workspaceCategoryFor(first)]
        2 * eventPublisher.publishEvent({
            it.categories().first().iconName() == "ShoppingCartOutlined"
        })
    }

    def "saveExpenseAll - should not save anything when every movement is a duplicate"() {
        given:
        def dto = new MovementToAdd(
                new BigDecimal("500.00"), LocalDate.now(), "Supermercado",
                [], "DEBITO", "EUR", 0, 0, "SANTANDER", null, null
        )
        def existing = buildMovement(1L)
        movementFactory.create(_ as MovementToAdd) >> existing
        movementDuplicateFilter.filterNew([existing], 1L) >> []

        when:
        def result = service.saveExpenseAll([dto])

        then:
        0 * movementRepository.saveAll(_ as List<Movement>)
        0 * eventPublisher.publishEvent(_ as MovementRecord)
        result == new ImportResultRecord(0, 1)
    }
}
