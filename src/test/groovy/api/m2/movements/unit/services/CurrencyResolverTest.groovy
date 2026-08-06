package api.m2.movements.unit.services

import api.m2.movements.entities.commons.Currency
import api.m2.movements.services.currencies.CurrencyAddService
import api.m2.movements.services.currencies.CurrencyResolver
import api.m2.movements.services.currencies.WorkspaceCurrencyService
import spock.lang.Specification

class CurrencyResolverTest extends Specification {

    CurrencyAddService currencyAddService = Mock(CurrencyAddService)
    WorkspaceCurrencyService workspaceCurrencyService = Mock(WorkspaceCurrencyService)

    CurrencyResolver resolver

    def setup() {
        resolver = new CurrencyResolver(currencyAddService, workspaceCurrencyService)
    }

    def "resolve - should find currency by symbol and ensure workspace association"() {
        given:
        def currency = Stub(Currency) { getSymbol() >> "USD" }
        currencyAddService.findBySymbol("USD") >> currency

        when:
        def result = resolver.resolve("USD", 1L)

        then:
        result == currency
        1 * workspaceCurrencyService.ensureCurrencyInWorkspace(1L, currency)
    }
}
