package api.m2.movements.unit.services

import api.m2.movements.entities.WorkspaceCategory
import api.m2.movements.entities.commons.Bank
import api.m2.movements.entities.commons.Category
import api.m2.movements.entities.commons.Currency
import api.m2.movements.entities.movements.Movement
import api.m2.movements.enums.MovementType
import api.m2.movements.mappers.CategoryMapper
import api.m2.movements.mappers.CurrencyMapper
import api.m2.movements.mappers.MovementMapperImpl
import api.m2.movements.records.categories.CategoryUpdateRecord
import api.m2.movements.records.movements.MovementRecord
import api.m2.movements.records.movements.MovementToAdd
import api.m2.movements.records.workspaces.WorkspaceBaseRecord
import api.m2.movements.repositories.MovementRepository
import api.m2.movements.repositories.WorkspaceCategoryRepository
import api.m2.movements.services.movements.MovementFactory
import api.m2.movements.services.movements.MovementItemService
import api.m2.movements.services.movements.MovementPersister
import api.m2.movements.services.movements.MovementRecordAssembler
import api.m2.movements.services.movements.SystemMovementAddService
import api.m2.movements.services.user.UserService
import api.m2.movements.services.workspaces.WorkspaceQueryService
import org.mapstruct.factory.Mappers
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.util.ReflectionTestUtils
import spock.lang.Specification

import java.time.LocalDate
import java.time.YearMonth

class SystemMovementAddServiceTest extends Specification {

    MovementRepository movementRepository = Mock(MovementRepository)
    MovementFactory movementFactory = Mock(MovementFactory)
    MovementItemService movementItemService = Mock(MovementItemService)
    ApplicationEventPublisher eventPublisher = Mock(ApplicationEventPublisher)
    WorkspaceCategoryRepository workspaceCategoryRepository = Mock(WorkspaceCategoryRepository)
    WorkspaceQueryService workspaceQueryService = Mock(WorkspaceQueryService)
    UserService userService = Mock(UserService)

    SystemMovementAddService service

    // Lo que el repositorio devuelve como "ya generado el mes siguiente"; el test de doble corrida lo va llenando.
    def storedInstallments = []

    def setup() {
        def movementMapper = new MovementMapperImpl()
        ReflectionTestUtils.setField(movementMapper, "categoryMapper", Mappers.getMapper(CategoryMapper))
        ReflectionTestUtils.setField(movementMapper, "currencyMapper", Mappers.getMapper(CurrencyMapper))

        service = new SystemMovementAddService(
                movementRepository,
                movementFactory,
                new MovementPersister(movementRepository, movementItemService, eventPublisher),
                new MovementRecordAssembler(movementMapper, Mappers.getMapper(CategoryMapper),
                        workspaceCategoryRepository, workspaceQueryService, userService)
        )
        workspaceCategoryRepository.findByWorkspaceIdAndIsActiveTrue(_ as Long) >> []
        movementItemService.getItems(_ as Long) >> []
        movementRepository.findCreditoInstallmentsBetween(_ as LocalDate, _ as LocalDate) >> { storedInstallments }
    }

    def buildMovement(Long workspaceId) {
        return Movement.builder()
                .id(42L)
                .amount(new BigDecimal("500.00"))
                .description("Ingreso recurrente")
                .date(LocalDate.now())
                .type(MovementType.INGRESO)
                .workspaceId(workspaceId)
                .categories([Category.builder().id(7L).description("HOGAR").build()] as Set)
                .currency(Currency.builder().id(1L).symbol("ARS").build())
                .ownerId(10L)
                .build()
    }

    def workspaceCategoryFor(Movement movement) {
        return WorkspaceCategory.builder()
                .workspaceId(movement.workspaceId)
                .category(movement.categories.first())
                .iconName("ShoppingCartOutlined")
                .iconColor("#ff0000")
                .build()
    }

    def "saveMovement - should save and publish the movement without identity metadata"() {
        given:
        def dto = new MovementToAdd(
                new BigDecimal("500.00"), LocalDate.now(), "Ingreso recurrente",
                ["HOGAR"], "INGRESO", "ARS", null, null, null, null, null
        )
        def movement = buildMovement(1L)

        movementFactory.create(dto, 1L, 10L) >> movement
        movementRepository.save(movement) >> movement

        when:
        def result = service.saveMovement(dto, 1L, 10L)

        then:
        1 * eventPublisher.publishEvent(_ as MovementRecord)
        0 * workspaceQueryService.findWorkspaceNameById(_ as Long)
        0 * userService.getUserNamesByIds(_ as List<Long>)
        result.metadata().workspace() == new WorkspaceBaseRecord(1L, null)
        result.metadata().owner().id() == 10L
        result.metadata().owner().givenName() == null
    }

    // Los iconos viven en WorkspaceCategory, no en Category: MovementMapper.toRecord los deja en
    // null, y el evento de websocket llegaba al front sin icono hasta el próximo refresh.
    def "saveMovement - should publish MovementRecord with the workspace's category icons"() {
        given:
        def dto = new MovementToAdd(
                new BigDecimal("500.00"), LocalDate.now(), "Cuota", ["HOGAR"], "GASTO", "ARS",
                null, null, null, null, null
        )
        def movement = buildMovement(1L)
        movementFactory.create(dto, 1L, 10L) >> movement
        movementRepository.save(movement) >> movement

        when:
        def result = service.saveMovement(dto, 1L, 10L)

        then:
        1 * workspaceCategoryRepository.findByWorkspaceIdAndIsActiveTrue(1L) >> [workspaceCategoryFor(movement)]
        result.categories().first().iconName() == "ShoppingCartOutlined"
        result.categories().first().iconColor() == "#ff0000"
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

    def existingInstallment(Integer cuotaActual) {
        return buildCreditoMovement(cuotaActual, 3, LocalDate.of(2026, 5, 15))
    }

    def "generateNextCreditInstallments - should not duplicate an installment already generated for the next month"() {
        given:
        def previous = buildCreditoMovement(1, 3, LocalDate.of(2026, 5, 15))
        movementRepository.findCreditoMovementsWithPendingInstallments(2026, 3) >> [previous]

        when:
        def count = service.generateNextCreditInstallments(YearMonth.of(2026, 3))

        then:
        // abril: ya existe la cuota 2 de esa misma compra
        1 * movementRepository.findCreditoInstallmentsBetween(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 1)) >>
                [existingInstallment(2)]
        count == 0
        0 * movementFactory.create(_ as MovementToAdd, _ as Long, _ as Long)
    }

    def "generateNextCreditInstallments - should generate only the purchases that are missing their next installment"() {
        given:
        def purchaseA = buildCreditoMovement(1, 3, LocalDate.of(2026, 5, 15))
        def purchaseB = buildCreditoMovement(1, 3, LocalDate.of(2026, 5, 15))
        purchaseB.description = "Celular"
        movementRepository.findCreditoMovementsWithPendingInstallments(2026, 3) >> [purchaseA, purchaseB]
        movementRepository.save(_ as Movement) >> { args -> args[0] }

        when:
        def count = service.generateNextCreditInstallments(YearMonth.of(2026, 3))

        then:
        1 * movementRepository.findCreditoInstallmentsBetween(_ as LocalDate, _ as LocalDate) >> [existingInstallment(2)]
        count == 1
        1 * movementFactory.create({ it.description() == "Celular" && it.cuotaActual() == 2 }, 5L, 10L) >> buildMovement(5L)
    }

    def "generateNextCreditInstallments - should keep identical purchases distinct when only one installment was generated"() {
        given:
        def first = buildCreditoMovement(1, 3, LocalDate.of(2026, 5, 15))
        def second = buildCreditoMovement(1, 3, LocalDate.of(2026, 5, 15))
        movementRepository.findCreditoMovementsWithPendingInstallments(2026, 3) >> [first, second]
        movementRepository.save(_ as Movement) >> { args -> args[0] }

        when:
        def count = service.generateNextCreditInstallments(YearMonth.of(2026, 3))

        then:
        1 * movementRepository.findCreditoInstallmentsBetween(_ as LocalDate, _ as LocalDate) >> [existingInstallment(2)]
        count == 1
        1 * movementFactory.create({ it.cuotaActual() == 2 }, 5L, 10L) >> buildMovement(5L)
    }

    def "generateNextCreditInstallments - running it twice generates each installment only once"() {
        given:
        def previous = buildCreditoMovement(1, 3, LocalDate.of(2026, 5, 15))
        movementRepository.findCreditoMovementsWithPendingInstallments(2026, 3) >> [previous]
        movementRepository.save(_ as Movement) >> { args -> args[0] }
        movementFactory.create(_ as MovementToAdd, 5L, 10L) >> buildMovement(5L)
        eventPublisher.publishEvent(_ as MovementRecord) >> { storedInstallments << existingInstallment(2) }

        when:
        def first = service.generateNextCreditInstallments(YearMonth.of(2026, 3))
        def second = service.generateNextCreditInstallments(YearMonth.of(2026, 3))

        then:
        first == 1
        second == 0
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
