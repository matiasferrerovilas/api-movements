package api.m2.movements.unit.services

import api.m2.movements.entities.commons.Bank
import api.m2.movements.entities.commons.Currency
import api.m2.movements.entities.movements.Income
import api.m2.movements.entities.movements.Movement
import api.m2.movements.enums.MovementType
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.records.categories.CategoryUpdateRecord
import api.m2.movements.records.currencies.CurrencyRecord
import api.m2.movements.records.income.IncomeToAdd
import api.m2.movements.records.movements.MovementToAdd
import api.m2.movements.repositories.IncomeRepository
import api.m2.movements.repositories.MovementRepository
import api.m2.movements.services.currencies.CurrencyAddService
import api.m2.movements.services.income.IncomeMovementService
import api.m2.movements.services.movements.MovementAddService
import api.m2.movements.services.movements.SystemMovementAddService
import spock.lang.Specification

import java.time.LocalDate
import java.time.ZoneOffset

class IncomeMovementServiceTest extends Specification {

    IncomeRepository incomeRepository = Mock(IncomeRepository)
    MovementRepository movementRepository = Mock(MovementRepository)
    CurrencyAddService currencyAddService = Mock(CurrencyAddService)
    MovementAddService movementAddService = Mock(MovementAddService)
    SystemMovementAddService systemMovementAddService = Mock(SystemMovementAddService)

    IncomeMovementService service

    // Lo que el repositorio devuelve como "ya generado este mes"; el test de doble corrida lo va llenando.
    def storedRecurring = []

    def setup() {
        service = new IncomeMovementService(
                incomeRepository,
                movementRepository,
                currencyAddService,
                movementAddService,
                systemMovementAddService
        )
        movementRepository.findIncomesByOwnerAndDescriptionBetween(
                _ as Long, _ as String, _ as LocalDate, _ as LocalDate) >> { storedRecurring }
    }

    // Note: membership check is handled by MembershipCheckAspect, not the service directly.

    def "reloadIncome - should save movement when called"() {
        given:
        def currency = Stub(Currency) { getSymbol() >> "ARS" }
        def bank = Stub(Bank) { getDescription() >> "GALICIA" }
        def income = new Income(id: 20L, amount: new BigDecimal("100000.00"), workspaceId: 2L, currency: currency, bank: bank)
        incomeRepository.findById(20L) >> Optional.of(income)

        when:
        service.reloadIncome(20L)

        then:
        1 * movementAddService.saveMovement(_)
    }

    def "reloadIncome - should throw EntityNotFoundException when income does not exist"() {
        given:
        incomeRepository.findById(999L) >> Optional.empty()

        when:
        service.reloadIncome(999L)

        then:
        thrown(EntityNotFoundException)
        0 * movementAddService.saveMovement(_)
    }

    def "addIngreso - should save movement with correct parameters"() {
        given:
        def incomeToAdd = new IncomeToAdd("GALICIA", new CurrencyRecord("EUR", null), new BigDecimal("1000.00"))
        def currency = Stub(Currency) { getSymbol() >> "EUR" }

        currencyAddService.findBySymbol("EUR") >> currency

        when:
        service.addIngreso(incomeToAdd)

        then:
        1 * movementAddService.saveMovement(_ as MovementToAdd) >> { List args ->
            def m = args[0] as MovementToAdd
            assert m.amount()        == new BigDecimal("1000.00")
            assert m.date()          == LocalDate.now(ZoneOffset.UTC)
            assert m.description()   == "Sueldo Recibido"
            assert m.categories() == [new CategoryUpdateRecord(null, "HOGAR")]
            assert m.type()          == MovementType.INGRESO.name()
            assert m.currency()      == "EUR"
            assert m.cuotaActual()   == null
            assert m.cuotasTotales() == null
            assert m.bank()          == "GALICIA"
        }
    }

    def "addIngreso - should always use HOGAR category"() {
        given:
        def incomeToAdd = new IncomeToAdd("BBVA", new CurrencyRecord("USD", null), new BigDecimal("500.00"))
        currencyAddService.findBySymbol("USD") >> Stub(Currency) { getSymbol() >> "USD" }

        when:
        service.addIngreso(incomeToAdd)

        then:
        1 * movementAddService.saveMovement({ MovementToAdd m -> m.categories() == [new CategoryUpdateRecord(null, "HOGAR")] })
    }

    def "addIngreso - should use today date in UTC"() {
        given:
        def incomeToAdd = new IncomeToAdd("BANCO_CIUDAD", new CurrencyRecord("ARS", null), new BigDecimal("200.00"))
        currencyAddService.findBySymbol(_) >> Stub(Currency) { getSymbol() >> "ARS" }

        when:
        service.addIngreso(incomeToAdd)

        then:
        1 * movementAddService.saveMovement({ MovementToAdd m -> m.date() == LocalDate.now(ZoneOffset.UTC) })
    }

    def "generateRecurringIncomeForUser - should generate movement for each income"() {
        given:
        def currency = Stub(Currency) { getSymbol() >> "ARS" }
        def bank = Stub(Bank) { getDescription() >> "GALICIA" }

        def income1 = new Income(id: 1L, amount: new BigDecimal("100000.00"), workspaceId: 10L, currency: currency, bank: bank, userId: 1L)
        def income2 = new Income(id: 2L, amount: new BigDecimal("50000.00"), workspaceId: 10L, currency: currency, bank: bank, userId: 1L)

        incomeRepository.findAllByUserId(1L) >> [income1, income2]

        when:
        def count = service.generateRecurringIncomeForUser(1L)

        then:
        count == 2
        2 * systemMovementAddService.saveMovement(_ as MovementToAdd, 10L, 1L)
    }

    def existingRecurring(Long workspaceId, String amount, String symbol, String bank) {
        return new Movement(workspaceId: workspaceId, amount: new BigDecimal(amount),
                currency: Stub(Currency) { getSymbol() >> symbol },
                bank: Stub(Bank) { getDescription() >> bank })
    }

    def "generateRecurringIncomeForUser - should not duplicate an income already generated this month"() {
        given:
        def currency = Stub(Currency) { getSymbol() >> "ARS" }
        def bank = Stub(Bank) { getDescription() >> "GALICIA" }
        def income = new Income(id: 1L, amount: new BigDecimal("100000.00"), workspaceId: 10L, currency: currency, bank: bank, userId: 1L)
        incomeRepository.findAllByUserId(1L) >> [income]

        when:
        def count = service.generateRecurringIncomeForUser(1L)

        then:
        // 100000.00 vs 100000: la comparación ignora la escala del BigDecimal
        1 * movementRepository.findIncomesByOwnerAndDescriptionBetween(
                1L, "Ingreso recurrente", _ as LocalDate, _ as LocalDate) >> [existingRecurring(10L, "100000", "ARS", "GALICIA")]
        count == 0
        0 * systemMovementAddService.saveMovement(_, _, _)
    }

    def "generateRecurringIncomeForUser - should generate only the incomes missing this month"() {
        given:
        def currency = Stub(Currency) { getSymbol() >> "ARS" }
        def bank = Stub(Bank) { getDescription() >> "GALICIA" }
        def salary = new Income(id: 1L, amount: new BigDecimal("100000.00"), workspaceId: 10L, currency: currency, bank: bank, userId: 1L)
        def rent = new Income(id: 2L, amount: new BigDecimal("50000.00"), workspaceId: 10L, currency: currency, bank: bank, userId: 1L)
        incomeRepository.findAllByUserId(1L) >> [salary, rent]

        when:
        def count = service.generateRecurringIncomeForUser(1L)

        then:
        1 * movementRepository.findIncomesByOwnerAndDescriptionBetween(
                1L, "Ingreso recurrente", _ as LocalDate, _ as LocalDate) >> [existingRecurring(10L, "100000", "ARS", "GALICIA")]
        count == 1
        1 * systemMovementAddService.saveMovement({ it.amount() == new BigDecimal("50000.00") }, 10L, 1L)
    }

    def "generateRecurringIncomeForUser - should keep two identical incomes distinct when only one was generated"() {
        given:
        def currency = Stub(Currency) { getSymbol() >> "ARS" }
        def bank = Stub(Bank) { getDescription() >> "GALICIA" }
        def first = new Income(id: 1L, amount: new BigDecimal("100.00"), workspaceId: 10L, currency: currency, bank: bank, userId: 1L)
        def second = new Income(id: 2L, amount: new BigDecimal("100.00"), workspaceId: 10L, currency: currency, bank: bank, userId: 1L)
        incomeRepository.findAllByUserId(1L) >> [first, second]

        when:
        def count = service.generateRecurringIncomeForUser(1L)

        then:
        1 * movementRepository.findIncomesByOwnerAndDescriptionBetween(
                1L, "Ingreso recurrente", _ as LocalDate, _ as LocalDate) >> [existingRecurring(10L, "100", "ARS", "GALICIA")]
        count == 1
        1 * systemMovementAddService.saveMovement(_ as MovementToAdd, 10L, 1L)
    }

    def "generateRecurringIncomeForUser - running it twice generates the incomes only once"() {
        given:
        def currency = Stub(Currency) { getSymbol() >> "ARS" }
        def bank = Stub(Bank) { getDescription() >> "GALICIA" }
        def income = new Income(id: 1L, amount: new BigDecimal("100.00"), workspaceId: 10L, currency: currency, bank: bank, userId: 1L)
        incomeRepository.findAllByUserId(1L) >> [income]
        systemMovementAddService.saveMovement(_ as MovementToAdd, _ as Long, _ as Long) >> { args ->
            storedRecurring << existingRecurring(10L, "100", "ARS", "GALICIA")
            null
        }

        when:
        def first = service.generateRecurringIncomeForUser(1L)
        def second = service.generateRecurringIncomeForUser(1L)

        then:
        first == 1
        second == 0
    }

    def "generateRecurringIncomeForUser - should return zero when user has no incomes"() {
        given:
        incomeRepository.findAllByUserId(1L) >> []

        when:
        def count = service.generateRecurringIncomeForUser(1L)

        then:
        count == 0
        0 * systemMovementAddService.saveMovement(_ as MovementToAdd, _ as Long, _ as Long)
    }

    def "generateRecurringIncomeForUser - should create movement with correct data"() {
        given:
        def currency = Stub(Currency) { getSymbol() >> "USD" }
        def bank = Stub(Bank) { getDescription() >> "BBVA" }

        def income = new Income(id: 1L, amount: new BigDecimal("2500.00"), workspaceId: 5L, currency: currency, bank: bank, userId: 1L)
        incomeRepository.findAllByUserId(1L) >> [income]

        when:
        service.generateRecurringIncomeForUser(1L)

        then:
        1 * systemMovementAddService.saveMovement(_ as MovementToAdd, 5L, 1L) >> { List args ->
            def m = args[0] as MovementToAdd
            assert m.amount() == new BigDecimal("2500.00")
            assert m.date() == LocalDate.now(ZoneOffset.UTC)
            assert m.description() == "Ingreso recurrente"
            assert m.categories() == [new CategoryUpdateRecord(null, "HOGAR")]
            assert m.type() == MovementType.INGRESO.name()
            assert m.currency() == "USD"
            assert m.bank() == "BBVA"
        }
    }

    def "generateRecurringIncomeForUser - should handle multiple incomes with different workspaces"() {
        given:
        def currency = Stub(Currency) { getSymbol() >> "ARS" }
        def bank = Stub(Bank) { getDescription() >> "GALICIA" }

        def income1 = new Income(id: 1L, amount: new BigDecimal("80000.00"), workspaceId: 10L, currency: currency, bank: bank, userId: 1L)
        def income2 = new Income(id: 2L, amount: new BigDecimal("20000.00"), workspaceId: 20L, currency: currency, bank: bank, userId: 1L)

        incomeRepository.findAllByUserId(1L) >> [income1, income2]

        when:
        def count = service.generateRecurringIncomeForUser(1L)

        then:
        count == 2
        1 * systemMovementAddService.saveMovement({ MovementToAdd m -> m.amount() == new BigDecimal("80000.00") }, 10L, 1L)
        1 * systemMovementAddService.saveMovement({ MovementToAdd m -> m.amount() == new BigDecimal("20000.00") }, 20L, 1L)
    }
}
