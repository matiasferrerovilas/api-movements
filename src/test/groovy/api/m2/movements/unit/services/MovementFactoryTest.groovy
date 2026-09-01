package api.m2.movements.unit.services

import api.m2.movements.entities.commons.Bank
import api.m2.movements.entities.commons.Category
import api.m2.movements.entities.commons.Currency
import api.m2.movements.entities.movements.Movement

import api.m2.movements.clients.identity.response.UserMe
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.exceptions.ExchangeRateNotFoundException
import api.m2.movements.mappers.MovementMapper
import api.m2.movements.records.categories.CategoryUpdateRecord
import api.m2.movements.records.movements.ExpenseToUpdate
import api.m2.movements.records.movements.MovementToAdd
import api.m2.movements.repositories.BankRepository
import api.m2.movements.services.category.CategoryResolver
import api.m2.movements.services.currencies.CurrencyResolver
import api.m2.movements.services.currencies.ExchangeRateResolver
import api.m2.movements.services.movements.MovementFactory
import api.m2.movements.services.user.UserService
import api.m2.movements.services.workspaces.WorkspaceContextService
import api.m2.movements.services.workspaces.WorkspaceQueryService
import spock.lang.Specification

import java.time.LocalDate

class MovementFactoryTest extends Specification {

    CategoryResolver categoryResolver = Mock(CategoryResolver)
    CurrencyResolver currencyResolver = Mock(CurrencyResolver)
    UserService userService = Mock(UserService)
    MovementMapper movementMapper = Mock(MovementMapper)
    WorkspaceContextService workspaceContextService = Mock(WorkspaceContextService)
    WorkspaceQueryService workspaceQueryService = Mock(WorkspaceQueryService)
    BankRepository bankRepository = Mock(BankRepository)
    ExchangeRateResolver exchangeRateResolver = Mock(ExchangeRateResolver)

    MovementFactory factory

    def setup() {
        factory = new MovementFactory(
                categoryResolver,
                currencyResolver,
                userService,
                movementMapper,
                workspaceContextService,
                workspaceQueryService,
                bankRepository,
                exchangeRateResolver
        )
    }

    def userMe(Long id) {
        return new UserMe(id, "user@test.com", "User", null, "PERSONAL", new UserMe.Metadata(false, true, [], null))
    }

    def "create - should build movement with all resolved dependencies"() {
        given:
        def dto = new MovementToAdd(
                new BigDecimal("100.00"),
                LocalDate.of(2024, 1, 15),
                "Test description",
                [new CategoryUpdateRecord(null, "HOGAR")],
                "GASTO",
                "USD",
                null,
                null,
                "BBVA",
                null
        )

        def movement = new Movement()
        def category = Stub(Category) { getId() >> 1L }
        def currency = Stub(Currency) { getSymbol() >> "USD" }
        def bank = Stub(Bank) { getId() >> 5L }

        movementMapper.toEntity(dto) >> movement
        workspaceContextService.getActiveWorkspaceId() >> 1L
        categoryResolver.resolveAll([new CategoryUpdateRecord(null, "HOGAR")], 1L) >> [category]
        currencyResolver.resolve("USD", 1L) >> currency
        userService.getMe() >> userMe(10L)
        bankRepository.findByDescription("BBVA") >> Optional.of(bank)
        exchangeRateResolver.resolveRate("USD", dto.date()) >> new BigDecimal("1.0")

        when:
        def result = factory.create(dto)

        then:
        result.categories == [category] as Set
        result.currency == currency
        result.ownerId == 10L
        result.workspaceId == 1L
        result.bank == bank
        result.exchangeRate == new BigDecimal("1.0")
    }

    def "create - should not set bank when bank is null in dto"() {
        given:
        def dto = new MovementToAdd(
                new BigDecimal("50.00"),
                LocalDate.of(2024, 2, 20),
                "No bank",
                [new CategoryUpdateRecord(null, "OCIO")],
                "GASTO",
                "EUR",
                null,
                null,
                null,
                null
        )

        def movement = new Movement()
        def category = Stub(Category)
        def currency = Stub(Currency) { getSymbol() >> "EUR" }

        movementMapper.toEntity(dto) >> movement
        workspaceContextService.getActiveWorkspaceId() >> 1L
        categoryResolver.resolveAll([new CategoryUpdateRecord(null, "OCIO")], 1L) >> [category]
        currencyResolver.resolve("EUR", 1L) >> currency
        userService.getMe() >> userMe(10L)
        exchangeRateResolver.resolveRate("EUR", dto.date()) >> new BigDecimal("1.08")

        when:
        def result = factory.create(dto)

        then:
        result.bank == null
        0 * bankRepository.findByDescription(_ as String)
    }

    def "create - should set null exchange rate when rate cannot be resolved"() {
        given:
        def dto = new MovementToAdd(
                new BigDecimal("100.00"),
                LocalDate.of(2024, 1, 15),
                "Test",
                [new CategoryUpdateRecord(null, "HOGAR")],
                "GASTO",
                "EUR",
                null,
                null,
                null,
                null
        )

        def movement = new Movement()
        def category = Stub(Category)
        def currency = Stub(Currency) { getSymbol() >> "EUR" }

        movementMapper.toEntity(dto) >> movement
        workspaceContextService.getActiveWorkspaceId() >> 1L
        categoryResolver.resolveAll([new CategoryUpdateRecord(null, "HOGAR")], 1L) >> [category]
        currencyResolver.resolve("EUR", 1L) >> currency
        userService.getMe() >> userMe(10L)
        exchangeRateResolver.resolveRate("EUR", dto.date()) >> {
            throw new ExchangeRateNotFoundException("No se encontró tasa de cambio para EUR en " + dto.date())
        }

        when:
        def result = factory.create(dto)

        then:
        result.exchangeRate == null
    }

    def "create - should compute lastCreditPayment for CREDITO as date + (cuotasTotales - cuotaActual) months"() {
        given:
        def dto = new MovementToAdd(
                new BigDecimal("300.00"),
                LocalDate.of(2026, 1, 15),
                "TV en cuotas",
                [new CategoryUpdateRecord(null, "HOGAR")],
                "CREDITO",
                "ARS",
                1,
                3,
                null,
                null
        )

        def movement = new Movement()
        def category = Stub(Category)
        def currency = Stub(Currency) { getSymbol() >> "ARS" }

        movementMapper.toEntity(dto) >> movement
        workspaceContextService.getActiveWorkspaceId() >> 1L
        categoryResolver.resolveAll([new CategoryUpdateRecord(null, "HOGAR")], 1L) >> [category]
        currencyResolver.resolve("ARS", 1L) >> currency
        userService.getMe() >> userMe(10L)
        exchangeRateResolver.resolveRate("ARS", dto.date()) >> new BigDecimal("1.0")

        when:
        def result = factory.create(dto)

        then:
        // enero (cuota 1 de 3) + 2 meses = marzo, la fecha de la última cuota
        result.lastCreditPayment == LocalDate.of(2026, 3, 15)
    }

    def "create - should use the dto's lastCreditPayment as-is instead of recomputing it, when already provided"() {
        given:
        def alreadyComputed = LocalDate.of(2026, 6, 1)
        def dto = new MovementToAdd(
                new BigDecimal("300.00"),
                LocalDate.of(2026, 4, 15),
                "TV en cuotas",
                [new CategoryUpdateRecord(null, "HOGAR")],
                "CREDITO",
                "ARS",
                3,
                3,
                null,
                alreadyComputed
        )

        def movement = new Movement()
        def category = Stub(Category)
        def currency = Stub(Currency) { getSymbol() >> "ARS" }

        movementMapper.toEntity(dto) >> movement
        workspaceContextService.getActiveWorkspaceId() >> 1L
        categoryResolver.resolveAll([new CategoryUpdateRecord(null, "HOGAR")], 1L) >> [category]
        currencyResolver.resolve("ARS", 1L) >> currency
        userService.getMe() >> userMe(10L)
        exchangeRateResolver.resolveRate("ARS", dto.date()) >> new BigDecimal("1.0")

        when:
        def result = factory.create(dto)

        then:
        // Si recalculara desde esta cuota (abril + 0 meses) daría abril, no junio — confirma que
        // se usa el valor ya provisto tal cual, como hace CreditInstallmentJob al clonar.
        result.lastCreditPayment == alreadyComputed
    }

    def "create - should leave lastCreditPayment null for non-CREDITO movements even with cuota info"() {
        given:
        def dto = new MovementToAdd(
                new BigDecimal("300.00"),
                LocalDate.of(2026, 1, 15),
                "Test",
                [new CategoryUpdateRecord(null, "HOGAR")],
                "GASTO",
                "ARS",
                1,
                3,
                null,
                null
        )

        def movement = new Movement()
        def category = Stub(Category)
        def currency = Stub(Currency) { getSymbol() >> "ARS" }

        movementMapper.toEntity(dto) >> movement
        workspaceContextService.getActiveWorkspaceId() >> 1L
        categoryResolver.resolveAll([new CategoryUpdateRecord(null, "HOGAR")], 1L) >> [category]
        currencyResolver.resolve("ARS", 1L) >> currency
        userService.getMe() >> userMe(10L)
        exchangeRateResolver.resolveRate("ARS", dto.date()) >> new BigDecimal("1.0")

        when:
        def result = factory.create(dto)

        then:
        result.lastCreditPayment == null
    }

    def "create - should throw EntityNotFoundException when bank not found"() {
        given:
        def dto = new MovementToAdd(
                new BigDecimal("100.00"),
                LocalDate.of(2024, 1, 15),
                "Test",
                [new CategoryUpdateRecord(null, "HOGAR")],
                "GASTO",
                "USD",
                null,
                null,
                "UNKNOWN_BANK",
                null
        )

        def movement = new Movement()
        def category = Stub(Category)
        def currency = Stub(Currency) { getSymbol() >> "USD" }

        movementMapper.toEntity(dto) >> movement
        workspaceContextService.getActiveWorkspaceId() >> 1L
        categoryResolver.resolveAll([new CategoryUpdateRecord(null, "HOGAR")], 1L) >> [category]
        currencyResolver.resolve("USD", 1L) >> currency
        userService.getMe() >> userMe(10L)
        bankRepository.findByDescription("UNKNOWN_BANK") >> Optional.empty()

        when:
        factory.create(dto)

        then:
        def ex = thrown(EntityNotFoundException)
        ex.message.contains("UNKNOWN_BANK")
    }

    def "applyUpdates - should update currency when provided"() {
        given:
        def newCurrency = Stub(Currency) { getSymbol() >> "ARS" }
        def dto = new ExpenseToUpdate(
                null,
                null,
                null,
                null,
                "ARS",
                null,
                null,
                null
        )
        def movement = new Movement()
        movement.setWorkspaceId(1L)

        currencyResolver.resolve("ARS", 1L) >> newCurrency

        when:
        factory.applyUpdates(dto, movement)

        then:
        movement.currency == newCurrency
    }

    def "applyUpdates - should update categories when provided"() {
        given:
        def newCategory = Stub(Category) { getDescription() >> "TRANSPORTE" }
        def categoryUpdateRecord = new CategoryUpdateRecord(5L, "TRANSPORTE")
        def dto = new ExpenseToUpdate(
                null,
                null,
                null,
                [categoryUpdateRecord],
                null,
                null,
                null,
                null
        )
        def movement = new Movement()

        categoryResolver.resolveAll([categoryUpdateRecord]) >> [newCategory]

        when:
        factory.applyUpdates(dto, movement)

        then:
        movement.categories == [newCategory] as Set
    }

    def "applyUpdates - should not update when fields are null"() {
        given:
        def existingCurrency = Stub(Currency) { getSymbol() >> "USD" }
        def existingCategory = Stub(Category) { getDescription() >> "HOGAR" }
        def dto = new ExpenseToUpdate(null, null, null, null, null, null, null, null)
        def movement = new Movement()
        movement.setCurrency(existingCurrency)
        movement.setCategories([existingCategory] as Set)

        when:
        factory.applyUpdates(dto, movement)

        then:
        movement.currency == existingCurrency
        movement.categories == [existingCategory] as Set
        0 * currencyResolver.resolve(_ as String, _ as Long)
        0 * categoryResolver.resolveAll(_ as List)
    }
}
