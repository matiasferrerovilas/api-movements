package api.m2.movements.unit.services

import api.m2.movements.entities.Bank
import api.m2.movements.entities.Currency
import api.m2.movements.entities.Income
import api.m2.movements.entities.User
import api.m2.movements.entities.Workspace
import api.m2.movements.enums.MovementType
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.mappers.IncomeMapper
import api.m2.movements.records.categories.CategoryRecord
import api.m2.movements.records.currencies.CurrencyRecord
import api.m2.movements.records.income.IncomeToAdd
import api.m2.movements.records.movements.MovementToAdd
import api.m2.movements.repositories.BankRepository
import api.m2.movements.repositories.IncomeRepository
import api.m2.movements.services.category.CategoryAddService
import api.m2.movements.services.currencies.CurrencyAddService
import api.m2.movements.services.workspaces.WorkspaceQueryService
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
    WorkspaceQueryService workspaceQueryService = Mock(WorkspaceQueryService)
    CurrencyAddService currencyAddService = Mock(CurrencyAddService)
    MovementAddService movementAddService = Mock(MovementAddService)
    BankRepository bankRepository = Mock(BankRepository)
    CategoryAddService categoryAddService = Mock(CategoryAddService)

    IncomeAddService service

    def setup() {
        incomeMapper = Mappers.getMapper(IncomeMapper)

        service = new IncomeAddService(
                incomeRepository,
                userService,
                incomeMapper,
                workspaceQueryService,
                currencyAddService,
                movementAddService,
                bankRepository,
                categoryAddService
        )
    }

    def "loadIncome - should set bank on income before saving"() {
        given:
        def incomeToAdd = new IncomeToAdd("galicia", new CurrencyRecord("ARS", null), new BigDecimal("150000.00"), "DEFAULT")
        def user = Stub(User)
        def workspace = Stub(Workspace)
        def currency = Stub(Currency)
        def bank = Bank.builder().id(1L).description("GALICIA").build()

        userService.getAuthenticatedUser() >> user
        workspaceQueryService.findWorkspaceByName("DEFAULT") >> workspace
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
        def incomeToAdd = new IncomeToAdd("  bbva  ", new CurrencyRecord("USD", null), new BigDecimal("500.00"), "DEFAULT")
        def bank = Bank.builder().id(2L).description("BBVA").build()

        userService.getAuthenticatedUser() >> Stub(User)
        workspaceQueryService.findWorkspaceByName("DEFAULT") >> Stub(Workspace)
        currencyAddService.findBySymbol("USD") >> Stub(Currency)
        bankRepository.findByDescription("BBVA") >> Optional.of(bank)

        when:
        service.loadIncome(incomeToAdd)

        then:
        1 * incomeRepository.save({ Income saved -> saved.bank == bank })
    }

    def "loadIncome - should throw EntityNotFoundException when bank does not exist"() {
        given:
        def incomeToAdd = new IncomeToAdd("BANCO_INEXISTENTE", new CurrencyRecord("ARS", null), new BigDecimal("100.00"), "DEFAULT")

        userService.getAuthenticatedUser() >> Stub(User)
        workspaceQueryService.findWorkspaceByName("DEFAULT") >> Stub(Workspace)
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
        def incomeToAdd = new IncomeToAdd("SANTANDER", new CurrencyRecord("ARS", null), new BigDecimal("200000.00"), "FAMILY")
        def user = Stub(User)
        def workspace = Stub(Workspace)
        def currency = Stub(Currency)
        def bank = Bank.builder().id(3L).description("SANTANDER").build()

        userService.getAuthenticatedUser() >> user
        workspaceQueryService.findWorkspaceByName("FAMILY") >> workspace
        currencyAddService.findBySymbol("ARS") >> currency
        bankRepository.findByDescription("SANTANDER") >> Optional.of(bank)

        when:
        service.loadIncome(incomeToAdd)

        then:
        1 * incomeRepository.save({ Income saved ->
            saved.user == user &&
            saved.workspace == workspace &&
            saved.currency == currency &&
            saved.bank == bank
        })
    }

    // --- deleteIncome ---
    // Note: membership check is handled by MembershipCheckAspect, not the service directly.

    def "deleteIncome - should delete income when called"() {
        given:
        def workspace = Stub(Workspace) { getId() >> 1L }
        def income = new Income(id: 10L, workspace: workspace)
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
        def workspace = Stub(Workspace) { getId() >> 2L }
        def currency = Stub(Currency) { getSymbol() >> "ARS" }
        def bank = Stub(Bank) { getDescription() >> "GALICIA" }
        def income = new Income(id: 20L, amount: new BigDecimal("100000.00"), workspace: workspace, currency: currency, bank: bank)
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
        def incomeToAdd = new IncomeToAdd("GALICIA", new CurrencyRecord("EUR", null), new BigDecimal("1000.00"), "Mi grupo")
        def category = Stub(CategoryRecord) { description() >> "HOGAR" }
        def workspace  = Stub(Workspace)        { getId()       >> 1L }
        def currency = Stub(Currency)       { getSymbol()   >> "EUR" }

        categoryAddService.findCategoryByDescription("HOGAR") >> category
        workspaceQueryService.findWorkspaceByName("Mi grupo") >> workspace
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
            assert m.cuotaActual()   == 0
            assert m.cuotasTotales() == 0
            assert m.bank()          == "GALICIA"
            assert m.workspaceId()   == 1L
        }
    }

    def "addIngreso - should always use HOGAR category"() {
        given:
        def incomeToAdd = new IncomeToAdd("BBVA", new CurrencyRecord("USD", null), new BigDecimal("500.00"), "Otro grupo")
        categoryAddService.findCategoryByDescription(_ as String) >> Stub(CategoryRecord) { description() >> "HOGAR" }
        workspaceQueryService.findWorkspaceByName("Otro grupo") >> Stub(Workspace) { getId() >> 2L }
        currencyAddService.findBySymbol("USD") >> Stub(Currency) { getSymbol() >> "USD" }

        when:
        service.addIngreso(incomeToAdd)

        then:
        1 * movementAddService.saveMovement({ MovementToAdd m -> m.category() == "HOGAR" })
    }

    def "addIngreso - should use today date in UTC"() {
        given:
        def incomeToAdd = new IncomeToAdd("BANCO_CIUDAD", new CurrencyRecord("ARS", null), new BigDecimal("200.00"), "Grupo ARS")
        categoryAddService.findCategoryByDescription(_) >> Stub(CategoryRecord) { description() >> "HOGAR" }
        workspaceQueryService.findWorkspaceByName(_)        >> Stub(Workspace)        { getId() >> 3L }
        currencyAddService.findBySymbol(_)              >> Stub(Currency)       { getSymbol() >> "ARS" }

        when:
        service.addIngreso(incomeToAdd)

        then:
        1 * movementAddService.saveMovement({ MovementToAdd m -> m.date() == LocalDate.now(ZoneOffset.UTC) })
    }
}
