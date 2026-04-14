package api.m2.movements.unit.services

import api.m2.movements.entities.Bank
import api.m2.movements.entities.Category
import api.m2.movements.entities.Currency
import api.m2.movements.entities.Movement
import api.m2.movements.entities.User
import api.m2.movements.entities.Workspace
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.mappers.MovementMapper
import api.m2.movements.records.categories.CategoryRecord
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
import spock.lang.Specification

import java.time.LocalDate

class MovementFactoryTest extends Specification {

    CategoryResolver categoryResolver = Mock(CategoryResolver)
    CurrencyResolver currencyResolver = Mock(CurrencyResolver)
    UserService userService = Mock(UserService)
    MovementMapper movementMapper = Mock(MovementMapper)
    WorkspaceContextService workspaceContextService = Mock(WorkspaceContextService)
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
                bankRepository,
                exchangeRateResolver
        )
    }

    def "create - should build movement with all resolved dependencies"() {
        given:
        def dto = new MovementToAdd(
                new BigDecimal("100.00"),
                LocalDate.of(2024, 1, 15),
                "Test description",
                "HOGAR",
                "GASTO",
                "USD",
                null,
                null,
                "BBVA"
        )

        def movement = new Movement()
        def category = Stub(Category) { getId() >> 1L }
        def currency = Stub(Currency) { getSymbol() >> "USD" }
        def user = Stub(User) { getId() >> 10L }
        def workspace = Stub(Workspace) { getId() >> 1L }
        def bank = Stub(Bank) { getId() >> 5L }

        movementMapper.toEntity(dto) >> movement
        workspaceContextService.getActiveWorkspace() >> workspace
        categoryResolver.resolve(_ as String, workspace) >> category
        currencyResolver.resolve("USD") >> currency
        userService.getAuthenticatedUser() >> user
        bankRepository.findByDescription("BBVA") >> Optional.of(bank)
        exchangeRateResolver.resolveRate("USD", dto.date()) >> new BigDecimal("1.0")

        when:
        def result = factory.create(dto)

        then:
        result.category == category
        result.currency == currency
        result.owner == user
        result.workspace == workspace
        result.bank == bank
        result.exchangeRate == new BigDecimal("1.0")
    }

    def "create - should not set bank when bank is null in dto"() {
        given:
        def dto = new MovementToAdd(
                new BigDecimal("50.00"),
                LocalDate.of(2024, 2, 20),
                "No bank",
                "OCIO",
                "GASTO",
                "EUR",
                null,
                null,
                null
        )

        def movement = new Movement()
        def category = Stub(Category)
        def currency = Stub(Currency) { getSymbol() >> "EUR" }
        def user = Stub(User)
        def workspace = Stub(Workspace)

        movementMapper.toEntity(dto) >> movement
        workspaceContextService.getActiveWorkspace() >> workspace
        categoryResolver.resolve(_ as String, workspace) >> category
        currencyResolver.resolve("EUR") >> currency
        userService.getAuthenticatedUser() >> user
        exchangeRateResolver.resolveRate("EUR", dto.date()) >> new BigDecimal("1.08")

        when:
        def result = factory.create(dto)

        then:
        result.bank == null
        0 * bankRepository.findByDescription(_ as String)
    }

    def "create - should throw EntityNotFoundException when bank not found"() {
        given:
        def dto = new MovementToAdd(
                new BigDecimal("100.00"),
                LocalDate.of(2024, 1, 15),
                "Test",
                "HOGAR",
                "GASTO",
                "USD",
                null,
                null,
                "UNKNOWN_BANK"
        )

        def movement = new Movement()
        def category = Stub(Category)
        def currency = Stub(Currency) { getSymbol() >> "USD" }
        def user = Stub(User)
        def workspace = Stub(Workspace)

        movementMapper.toEntity(dto) >> movement
        workspaceContextService.getActiveWorkspace() >> workspace
        categoryResolver.resolve(_ as String, workspace) >> category
        currencyResolver.resolve("USD") >> currency
        userService.getAuthenticatedUser() >> user
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

        currencyResolver.resolve("ARS") >> newCurrency

        when:
        factory.applyUpdates(dto, movement)

        then:
        movement.currency == newCurrency
    }

    def "applyUpdates - should update category when provided"() {
        given:
        def newCategory = Stub(Category) { getDescription() >> "TRANSPORTE" }
        def categoryUpdateRecord = new CategoryUpdateRecord(5L, "TRANSPORTE")
        def dto = new ExpenseToUpdate(
                null,
                null,
                null,
                categoryUpdateRecord,
                null,
                null,
                null,
                null
        )
        def movement = new Movement()

        categoryResolver.resolve(_ as CategoryUpdateRecord) >> newCategory

        when:
        factory.applyUpdates(dto, movement)

        then:
        movement.category == newCategory
    }

    def "applyUpdates - should not update when fields are null"() {
        given:
        def existingCurrency = Stub(Currency) { getSymbol() >> "USD" }
        def existingCategory = Stub(Category) { getDescription() >> "HOGAR" }
        def dto = new ExpenseToUpdate(null, null, null, null, null, null, null, null)
        def movement = new Movement()
        movement.setCurrency(existingCurrency)
        movement.setCategory(existingCategory)

        when:
        factory.applyUpdates(dto, movement)

        then:
        movement.currency == existingCurrency
        movement.category == existingCategory
        0 * currencyResolver.resolve(_ as String)
        0 * categoryResolver.resolve(_ as String)
    }
}
