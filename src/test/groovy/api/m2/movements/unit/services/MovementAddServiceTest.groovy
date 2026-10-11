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
import api.m2.movements.records.categories.CategoryCorrectedEvent
import api.m2.movements.records.categories.CategoryUpdateRecord
import api.m2.movements.records.movements.ExpenseToUpdate
import api.m2.movements.records.movements.MovementDeletedEvent
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
                movementMapper,
                movementFactory,
                movementItemService,
                eventPublisher,
                workspaceQueryService,
                userService,
                movementDuplicateFilter,
                new MovementPersister(movementRepository, movementItemService, eventPublisher),
                new MovementRecordAssembler(movementMapper, Mappers.getMapper(CategoryMapper), workspaceCategoryRepository)
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

    // Note: membership check is handled by MembershipCheckAspect, not the service directly.

    def "updateMovement - should update movement when called"() {
        given:
        def dto = new ExpenseToUpdate(null, null, null, null, null, null, null, null, null, null)
        def movement = buildMovement(1L)
        movementRepository.findById(10L) >> Optional.of(movement)

        when:
        service.updateMovement(dto, 10L)

        then:
        1 * movementRepository.save(_ as Movement)
    }

    def "updateMovement - should publish a category correction when the category changed"() {
        given:
        def dto = new ExpenseToUpdate(null, null, null, [new CategoryUpdateRecord(null, "SUPERMERCADO")], null, null, null, null, null, null)
        def movement = buildMovement(1L)
        movement.categories = [Category.builder().id(1L).description("SIN CATEGORIA").build()] as Set
        movementRepository.findById(10L) >> Optional.of(movement)
        movementFactory.applyUpdates(dto, movement) >> {
            movement.categories = [Category.builder().id(5L).description("SUPERMERCADO").build()] as Set
        }

        when:
        service.updateMovement(dto, 10L)

        then:
        1 * eventPublisher.publishEvent(_ as CategoryCorrectedEvent) >> { List args ->
            def event = args[0] as CategoryCorrectedEvent
            assert event.workspaceId() == 1L
            assert event.movementDescription() == "Supermercado"
            assert event.categoryId() == 5L
        }
    }

    def "updateMovement - should not publish a category correction when categories did not change"() {
        given:
        def dto = new ExpenseToUpdate(null, null, null, [new CategoryUpdateRecord(null, "HOGAR")], null, null, null, null, null, null)
        def movement = buildMovement(1L)
        movement.categories = [Category.builder().id(2L).description("HOGAR").build()] as Set
        movementRepository.findById(10L) >> Optional.of(movement)
        movementFactory.applyUpdates(dto, movement) >> {
            movement.categories = [Category.builder().id(2L).description("HOGAR").build()] as Set
        }

        when:
        service.updateMovement(dto, 10L)

        then:
        0 * eventPublisher.publishEvent(_ as CategoryCorrectedEvent)
    }

    def "updateMovement - should not publish a category correction when the movement ends with several categories"() {
        given:
        def dto = new ExpenseToUpdate(null, null, null, [new CategoryUpdateRecord(null, "HOGAR"), new CategoryUpdateRecord(null, "SALUD")],
                null, null, null, null, null, null)
        def movement = buildMovement(1L)
        movement.categories = [Category.builder().id(1L).description("SIN CATEGORIA").build()] as Set
        movementRepository.findById(10L) >> Optional.of(movement)
        movementFactory.applyUpdates(dto, movement) >> {
            movement.categories = [Category.builder().id(2L).description("HOGAR").build(),
                                   Category.builder().id(3L).description("SALUD").build()] as Set
        }

        when:
        service.updateMovement(dto, 10L)

        then:
        0 * eventPublisher.publishEvent(_ as CategoryCorrectedEvent)
    }

    def "updateMovement - should throw EntityNotFoundException when movement does not exist"() {
        given:
        def dto = new ExpenseToUpdate(null, null, null, null, null, null, null, null, null, null)
        movementRepository.findById(999L) >> Optional.empty()

        when:
        service.updateMovement(dto, 999L)

        then:
        thrown(EntityNotFoundException)
        0 * movementRepository.save(_ as Movement)
    }

    // El type-change side effect vive en MovementMapper (updateMovement), no en MovementFactory
    // — se prueba acá directo contra el mapper real (no mockeado) que usa este test.

    def "updateMovement mapper - should not touch type when not provided"() {
        given:
        def dto = new ExpenseToUpdate(null, null, null, null, null, null, null, null, null, null)
        def movement = new Movement()
        movement.setType(MovementType.DEBITO)

        when:
        movementMapper.updateMovement(dto, movement)

        then:
        movement.type == MovementType.DEBITO
    }

    def "updateMovement mapper - should change type between non-CREDITO types without touching cuotas"() {
        given:
        def dto = new ExpenseToUpdate(null, null, null, null, null, null, null, null, "INGRESO", null)
        def movement = new Movement()
        movement.setType(MovementType.DEBITO)

        when:
        movementMapper.updateMovement(dto, movement)

        then:
        movement.type == MovementType.INGRESO
        movement.cuotaActual == null
        movement.cuotasTotales == null
        movement.lastCreditPayment == null
    }

    def "updateMovement mapper - moving out of CREDITO clears cuotaActual, cuotasTotales and lastCreditPayment"() {
        given:
        def dto = new ExpenseToUpdate(null, null, null, null, null, null, null, null, "DEBITO", null)
        def movement = new Movement()
        movement.setType(MovementType.CREDITO)
        movement.setCuotaActual(2)
        movement.setCuotasTotales(6)
        movement.setLastCreditPayment(LocalDate.of(2026, 6, 1))

        when:
        movementMapper.updateMovement(dto, movement)

        then:
        movement.type == MovementType.DEBITO
        movement.cuotaActual == null
        movement.cuotasTotales == null
        movement.lastCreditPayment == null
    }

    def "updateMovement mapper - moving into CREDITO paints cuotaActual/cuotasTotales from dto and leaves lastCreditPayment untouched"() {
        given:
        def dto = new ExpenseToUpdate(null, null, null, null, null, 2, 6, null, "CREDITO", null)
        def movement = new Movement()
        movement.setType(MovementType.DEBITO)
        // Valor previo cualquiera: no se debe recalcular acá, queda tal cual estaba.
        movement.setLastCreditPayment(LocalDate.of(2099, 1, 1))

        when:
        movementMapper.updateMovement(dto, movement)

        then:
        movement.type == MovementType.CREDITO
        movement.cuotaActual == 2
        movement.cuotasTotales == 6
        movement.lastCreditPayment == LocalDate.of(2099, 1, 1)
    }

    def "updateMovement mapper - nulls cuotaActual/cuotasTotales when dto sends them null even without a type change"() {
        given:
        def dto = new ExpenseToUpdate(null, null, null, null, null, null, null, null, null, null)
        def movement = new Movement()
        movement.setType(MovementType.CREDITO)
        movement.setCuotaActual(2)
        movement.setCuotasTotales(6)
        movement.setLastCreditPayment(LocalDate.of(2026, 6, 1))

        when:
        movementMapper.updateMovement(dto, movement)

        then:
        movement.type == MovementType.CREDITO
        movement.cuotaActual == null
        movement.cuotasTotales == null
        // No se toca porque el type no vino en el dto (sin señal explícita de cambio).
        movement.lastCreditPayment == LocalDate.of(2026, 6, 1)
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
