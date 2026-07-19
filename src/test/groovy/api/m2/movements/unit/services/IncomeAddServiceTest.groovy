package api.m2.movements.unit.services

import api.m2.movements.entities.commons.Bank
import api.m2.movements.entities.commons.Currency
import api.m2.movements.entities.movements.Income

import api.m2.movements.clients.identity.response.UserMe
import api.m2.movements.enums.MovementType
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.mappers.IncomeMapper
import api.m2.movements.records.currencies.CurrencyRecord
import api.m2.movements.records.income.IncomeToAdd
import api.m2.movements.records.movements.MovementToAdd
import api.m2.movements.repositories.BankRepository
import api.m2.movements.repositories.IncomeRepository
import api.m2.movements.services.currencies.CurrencyAddService
import api.m2.movements.services.workspaces.WorkspaceContextService
import api.m2.movements.services.income.IncomeAddService
import api.m2.movements.services.movements.MovementAddService
import api.m2.movements.services.user.UserService
import org.mapstruct.factory.Mappers
import spock.lang.Specification

import java.time.LocalDate
import java.time.ZoneOffset

class IncomeAddServiceTest extends Specification {

    IncomeRepository incomeRepository = Mock(IncomeRepository)
    UserService userService = Mock(UserService)
    IncomeMapper incomeMapper
    WorkspaceContextService workspaceContextService = Mock(WorkspaceContextService)
    CurrencyAddService currencyAddService = Mock(CurrencyAddService)
    MovementAddService movementAddService = Mock(MovementAddService)
    BankRepository bankRepository = Mock(BankRepository)

    IncomeAddService service

    def setup() {
        incomeMapper = Mappers.getMapper(IncomeMapper)

        service = new IncomeAddService(
                incomeRepository,
                userService,
                incomeMapper,
                workspaceContextService,
                currencyAddService,
                movementAddService,
                bankRepository
        )
    }

    def userMe(Long id) {
        return new UserMe(id, "user@test.com", "User", null, "PERSONAL", new UserMe.Metadata(false, true, []))
    }

    def "loadIncome - should set bank on income before saving"() {
        given:
        def incomeToAdd = new IncomeToAdd("galicia", new CurrencyRecord("ARS", null), new BigDecimal("150000.00"))
        def currency = Stub(Currency)
        def bank = Bank.builder().id(1L).description("GALICIA").build()

        userService.getMe() >> userMe(1L)
        workspaceContextService.getActiveWorkspaceId() >> 1L
        currencyAddService.findBySymbol("ARS") >> currency
        bankRepository.findByDescription("GALICIA") >> Optional.of(bank)

        when:
        service.loadIncome(incomeToAdd)

        then:
        1 * incomeRepository.save({ Income saved ->
            saved.bank == bank
        })
    }

    def "loadIncome - should sanitize bank name (trim + uppercase) before lookup"() {
        given:
        def incomeToAdd = new IncomeToAdd("  bbva  ", new CurrencyRecord("USD", null), new BigDecimal("500.00"))
        def bank = Bank.builder().id(2L).description("BBVA").build()

        userService.getMe() >> userMe(1L)
        workspaceContextService.getActiveWorkspaceId() >> 1L
        currencyAddService.findBySymbol("USD") >> Stub(Currency)
        bankRepository.findByDescription("BBVA") >> Optional.of(bank)

        when:
        service.loadIncome(incomeToAdd)

        then:
        1 * incomeRepository.save({ Income saved -> saved.bank == bank })
    }

    def "loadIncome - should throw EntityNotFoundException when bank does not exist"() {
        given:
        def incomeToAdd = new IncomeToAdd("BANCO_INEXISTENTE", new CurrencyRecord("ARS", null), new BigDecimal("100.00"))

        userService.getMe() >> userMe(1L)
        workspaceContextService.getActiveWorkspaceId() >> 1L
        currencyAddService.findBySymbol("ARS") >> Stub(Currency)
        bankRepository.findByDescription("BANCO_INEXISTENTE") >> Optional.empty()

        when:
        service.loadIncome(incomeToAdd)

        then:
        thrown(EntityNotFoundException)
        0 * incomeRepository.save(_)
    }

    def "loadIncome - should set user, workspace and currency on income"() {
        given:
        def incomeToAdd = new IncomeToAdd("SANTANDER", new CurrencyRecord("ARS", null), new BigDecimal("200000.00"))
        def currency = Stub(Currency)
        def bank = Bank.builder().id(3L).description("SANTANDER").build()

        userService.getMe() >> userMe(1L)
        workspaceContextService.getActiveWorkspaceId() >> 1L
        currencyAddService.findBySymbol("ARS") >> currency
        bankRepository.findByDescription("SANTANDER") >> Optional.of(bank)

        when:
        service.loadIncome(incomeToAdd)

        then:
        1 * incomeRepository.save({ Income saved ->
            saved.userId == 1L &&
            saved.workspaceId == 1L &&
            saved.currency == currency &&
            saved.bank == bank
        })
    }

    // --- deleteIncome ---
    // Note: membership check is handled by MembershipCheckAspect, not the service directly.

    def "deleteIncome - should delete income when called"() {
        given:
        def income = new Income(id: 10L, workspaceId: 1L)
        incomeRepository.findById(10L) >> Optional.of(income)

        when:
        service.deleteIncome(10L)

        then:
        1 * incomeRepository.delete(income)
    }

    def "deleteIncome - should throw EntityNotFoundException when income does not exist"() {
        given:
        incomeRepository.findById(999L) >> Optional.empty()

        when:
        service.deleteIncome(999L)

        then:
        thrown(EntityNotFoundException)
        0 * incomeRepository.delete(_ as Income)
    }

    // --- reloadIncome ---
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

    // --- addIngreso ---

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
            assert m.category()      == "HOGAR"
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
        1 * movementAddService.saveMovement({ MovementToAdd m -> m.category() == "HOGAR" })
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

    // --- generateRecurringIncomeForUser ---

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
        2 * movementAddService.saveMovement(_ as MovementToAdd, 10L, 1L)
    }

    def "generateRecurringIncomeForUser - should return zero when user has no incomes"() {
        given:
        incomeRepository.findAllByUserId(1L) >> []

        when:
        def count = service.generateRecurringIncomeForUser(1L)

        then:
        count == 0
        0 * movementAddService.saveMovement(_ as MovementToAdd, _ as Long, _ as Long)
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
        1 * movementAddService.saveMovement(_ as MovementToAdd, 5L, 1L) >> { List args ->
            def m = args[0] as MovementToAdd
            assert m.amount() == new BigDecimal("2500.00")
            assert m.date() == LocalDate.now(ZoneOffset.UTC)
            assert m.description() == "Ingreso recurrente"
            assert m.category() == "HOGAR"
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
        1 * movementAddService.saveMovement({ MovementToAdd m -> m.amount() == new BigDecimal("80000.00") }, 10L, 1L)
        1 * movementAddService.saveMovement({ MovementToAdd m -> m.amount() == new BigDecimal("20000.00") }, 20L, 1L)
    }
}
