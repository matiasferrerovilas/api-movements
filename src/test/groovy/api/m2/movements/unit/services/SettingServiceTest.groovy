package api.m2.movements.unit.services

import api.m2.movements.services.settings.SettingService
import api.m2.movements.entities.Account
import api.m2.movements.entities.Currency
import api.m2.movements.enums.MovementType
import api.m2.movements.records.categories.CategoryRecord
import api.m2.movements.records.income.IncomeToAdd
import api.m2.movements.records.movements.MovementToAdd
import api.m2.movements.services.category.CategoryAddService
import api.m2.movements.services.currencies.CurrencyAddService
import api.m2.movements.services.groups.AccountQueryService
import api.m2.movements.services.movements.MovementAddService
import spock.lang.Specification
import java.time.LocalDate
import java.time.ZoneOffset

class SettingServiceTest extends Specification {

    MovementAddService movementAddService = Mock(MovementAddService)
    CategoryAddService categoryAddService = Mock(CategoryAddService)
    CurrencyAddService currencyAddService = Mock(CurrencyAddService)
    AccountQueryService accountQueryService = Mock(AccountQueryService)

    SettingService service

    def setup() {
        service = new SettingService(
                movementAddService,
                categoryAddService,
                currencyAddService,
                accountQueryService
        )
    }

    def "addIngreso - should save movement with correct parameters"() {
        given:
        def incomeToAdd = new IncomeToAdd("GALICIA", "EUR", new BigDecimal("1000.00"), "Mi grupo")
        def category = Stub(CategoryRecord) { description() >> "HOGAR" }
        def account  = Stub(Account)        { getId()       >> 1L }
        def currency = Stub(Currency)       { getSymbol()   >> "EUR" }

        categoryAddService.findCategoryByDescription("HOGAR") >> category
        accountQueryService.findAccountByName("Mi grupo") >> account
        currencyAddService.findBySymbol("EUR") >> currency

        when:
        service.addIngreso(incomeToAdd)

        then:
        1 * movementAddService.saveMovement(_ as MovementToAdd) >> { List args ->
            def m = args[0] as MovementToAdd
            assert m.amount()      == new BigDecimal("1000.00")
            assert m.date()        == LocalDate.now(ZoneOffset.UTC)
            assert m.description() == "Sueldo Recibido"
            assert m.category()     == "HOGAR"
            assert m.type()         == MovementType.INGRESO.name()
            assert m.currency()     == "EUR"
            assert m.cuotaActual()  == 0
            assert m.cuotasTotales() == 0
            assert m.bank()         == "GALICIA"
            assert m.groupId()      == 1L
        }
    }

    def "addIngreso - should always use HOGAR category"() {
        given:
        def incomeToAdd = new IncomeToAdd("BBVA", "USD", new BigDecimal("500.00"), "Otro grupo")
        categoryAddService.findCategoryByDescription(_ as String) >> Stub(CategoryRecord) { description() >> "HOGAR" }
        accountQueryService.findAccountByName("Otro grupo") >> Stub(Account) { getId() >> 2L }
        currencyAddService.findBySymbol("USD") >> Stub(Currency) { getSymbol() >> "USD" }

        when:
        service.addIngreso(incomeToAdd)

        then:
        1 * movementAddService.saveMovement({ MovementToAdd m ->
            m.category() == "HOGAR"
        })
    }

    def "addIngreso - should use today date in UTC"() {
        given:
        def incomeToAdd = new IncomeToAdd("BANCO_CIUDAD", "ARS", new BigDecimal("200.00"), "Grupo ARS")
        categoryAddService.findCategoryByDescription(_) >> Stub(CategoryRecord) { description() >> "HOGAR" }
        accountQueryService.findAccountByName(_)        >> Stub(Account)        { getId() >> 3L }
        currencyAddService.findBySymbol(_)              >> Stub(Currency)       { getSymbol() >> "ARS" }

        when:
        service.addIngreso(incomeToAdd)

        then:
        1 * movementAddService.saveMovement({ MovementToAdd m -> m.date() == LocalDate.now(ZoneOffset.UTC) })
    }
}