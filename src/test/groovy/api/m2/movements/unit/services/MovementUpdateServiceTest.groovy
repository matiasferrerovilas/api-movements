package api.m2.movements.unit.services

import api.m2.movements.entities.commons.Category
import api.m2.movements.entities.commons.Currency
import api.m2.movements.entities.movements.Movement

import api.m2.movements.enums.MovementType
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.mappers.CategoryMapper
import api.m2.movements.mappers.CurrencyMapper
import api.m2.movements.mappers.MovementMapper
import api.m2.movements.mappers.MovementMapperImpl
import api.m2.movements.events.CategoryCorrectedEvent
import api.m2.movements.records.categories.CategoryUpdateRecord
import api.m2.movements.records.movements.ExpenseToUpdate
import api.m2.movements.repositories.MovementRepository
import api.m2.movements.services.movements.MovementFactory
import api.m2.movements.services.movements.MovementItemService
import org.mapstruct.factory.Mappers
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.util.ReflectionTestUtils
import api.m2.movements.services.movements.MovementUpdateService
import spock.lang.Specification

import java.time.LocalDate

class MovementUpdateServiceTest extends Specification {

    MovementRepository movementRepository = Mock(MovementRepository)
    MovementMapper movementMapper
    MovementFactory movementFactory = Mock(MovementFactory)
    MovementItemService movementItemService = Mock(MovementItemService)
    ApplicationEventPublisher eventPublisher = Mock(ApplicationEventPublisher)

    MovementUpdateService service

    def setup() {
        movementMapper = new MovementMapperImpl()
        ReflectionTestUtils.setField(movementMapper, "categoryMapper", Mappers.getMapper(CategoryMapper))
        ReflectionTestUtils.setField(movementMapper, "currencyMapper", Mappers.getMapper(CurrencyMapper))

        service = new MovementUpdateService(
                movementRepository,
                movementMapper,
                movementFactory,
                movementItemService,
                eventPublisher
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
}
