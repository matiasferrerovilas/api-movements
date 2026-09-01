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
import api.m2.movements.records.categories.CategoryUpdateRecord
import api.m2.movements.records.movements.ExpenseToUpdate
import api.m2.movements.records.movements.MovementDeletedEvent
import api.m2.movements.records.movements.MovementRecord
import api.m2.movements.records.movements.MovementToAdd
import api.m2.movements.records.workspaces.WorkspaceBaseRecord
import api.m2.movements.repositories.MovementRepository
import api.m2.movements.services.movements.MovementAddService
import api.m2.movements.services.movements.MovementFactory
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
                ["HOGAR"], "GASTO", "ARS", null, null, null, null
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
                ["HOGAR"], "GASTO", "ARS", null, null, null, null
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

    def "saveMovement - should throw PermissionDeniedException when user is not a member of the workspace"() {
        given:
        def dto = new MovementToAdd(
                new BigDecimal("500.00"), LocalDate.now(), "Supermercado",
                ["HOGAR"], "GASTO", "ARS", null, null, null, null
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

    def "saveSystemMovement - should save movement without calling identity"() {
        given:
        def dto = new MovementToAdd(
                new BigDecimal("500.00"), LocalDate.now(), "Ingreso recurrente",
                ["HOGAR"], "INGRESO", "ARS", null, null, null, null
        )
        def movement = buildMovement(1L)

        movementFactory.create(dto, 1L, 10L) >> movement
        movementRepository.save(movement) >> movement

        when:
        def result = service.saveSystemMovement(dto, 1L, 10L)

        then:
        1 * eventPublisher.publishEvent(_ as MovementRecord)
        0 * workspaceQueryService.findWorkspaceNameById(_ as Long)
        0 * userService.getUserNamesByIds(_ as List<Long>)
        result.metadata().workspace() == new WorkspaceBaseRecord(1L, null)
        result.metadata().owner().id() == 10L
        result.metadata().owner().givenName() == null
    }

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

    def buildCreditoMovement(Integer cuotaActual, Integer cuotasTotales, LocalDate lastCreditPayment) {
        return Movement.builder()
                .id(7L)
                .amount(new BigDecimal("1000.00"))
                .description("TV en cuotas")
                .date(LocalDate.of(2026, 3, 15))
                .type(MovementType.CREDITO)
                .workspaceId(5L)
                .ownerId(10L)
                .categories([Category.builder().id(1L).description("HOGAR").build()] as Set)
                .currency(Currency.builder().id(1L).symbol("ARS").build())
                .bank(Bank.builder().id(1L).description("GALICIA").build())
                .cuotaActual(cuotaActual)
                .cuotasTotales(cuotasTotales)
                .lastCreditPayment(lastCreditPayment)
                .build()
    }

    def "generateNextCreditInstallments - should generate the next cuota for each pending CREDITO movement from the given month"() {
        given:
        def previous = buildCreditoMovement(1, 3, LocalDate.of(2026, 5, 15))
        movementRepository.findCreditoMovementsWithPendingInstallments(2026, 3) >> [previous]
        movementRepository.save(_ as Movement) >> { args -> args[0] }

        when:
        def count = service.generateNextCreditInstallments(YearMonth.of(2026, 3))

        then:
        count == 1
        // El stub y el chequeo van en la misma interacción: separarlos en un given: aparte y un
        // 1 * ... en then: hace que la interacción de then: (sin >>) le gane al stub de given:,
        // devolviendo null en vez del movement esperado.
        1 * movementFactory.create({ MovementToAdd dto ->
            dto.cuotaActual() == 2 &&
            dto.cuotasTotales() == 3 &&
            dto.amount() == new BigDecimal("1000.00") &&
            dto.description() == "TV en cuotas" &&
            dto.type() == MovementType.CREDITO.name() &&
            dto.currency() == "ARS" &&
            dto.bank() == "GALICIA" &&
            dto.categories() == [new CategoryUpdateRecord(null, "HOGAR")] &&
            // La fecha de la última cuota se copia tal cual de la cuota anterior, nunca se
            // recalcula acá — es MovementFactory el único lugar que decide entre copiar o calcular.
            dto.lastCreditPayment() == LocalDate.of(2026, 5, 15)
        }, 5L, 10L) >> buildMovement(5L)
    }

    def "generateNextCreditInstallments - should return zero and create nothing when there are no pending installments"() {
        given:
        movementRepository.findCreditoMovementsWithPendingInstallments(2026, 3) >> []

        when:
        def count = service.generateNextCreditInstallments(YearMonth.of(2026, 3))

        then:
        count == 0
        0 * movementFactory.create(_ as MovementToAdd, _ as Long, _ as Long)
    }

    def "generateNextCreditInstallments - should generate one movement per pending purchase, each with its own workspace and owner"() {
        given:
        def purchaseA = buildCreditoMovement(2, 6, LocalDate.of(2026, 7, 1))
        def purchaseB = Movement.builder()
                .id(8L)
                .amount(new BigDecimal("500.00"))
                .description("Celular")
                .date(LocalDate.of(2026, 3, 20))
                .type(MovementType.CREDITO)
                .workspaceId(9L)
                .ownerId(11L)
                .categories([Category.builder().id(2L).description("TECNOLOGIA").build()] as Set)
                .currency(Currency.builder().id(1L).symbol("ARS").build())
                .cuotaActual(4)
                .cuotasTotales(6)
                .lastCreditPayment(LocalDate.of(2026, 6, 20))
                .build()
        movementRepository.findCreditoMovementsWithPendingInstallments(2026, 3) >> [purchaseA, purchaseB]
        movementRepository.save(_ as Movement) >> { args -> args[0] }

        when:
        def count = service.generateNextCreditInstallments(YearMonth.of(2026, 3))

        then:
        count == 2
        1 * movementFactory.create({ it.cuotaActual() == 3 }, 5L, 10L) >> buildMovement(5L)
        1 * movementFactory.create({ it.cuotaActual() == 5 && it.bank() == null }, 9L, 11L) >> buildMovement(9L)
    }
}

