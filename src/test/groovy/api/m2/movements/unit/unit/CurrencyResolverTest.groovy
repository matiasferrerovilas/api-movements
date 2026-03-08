package api.m2.movements.unit.unit

import api.m2.movements.entities.Currency
import api.m2.movements.services.currencies.CurrencyAddService
import api.m2.movements.services.currencies.CurrencyResolver
import spock.lang.Specification
import spock.lang.Subject

class CurrencyResolverTest extends Specification {

    CurrencyAddService currencyAddService = Mock(CurrencyAddService)
    @Subject
    CurrencyResolver service

    def setup() {
        service = new CurrencyResolver(currencyAddService)
    }

    def "resolve - should return currency by symbol"() {
        given:
        def currency = Stub(Currency) {
            getId() >> 1L
            getDescription() >> "Euro"
            getSymbol() >> "EUR"
            getEnabled() >> true
        }
        def symbol = "EUR"

        when:
        currencyAddService.findBySymbol(symbol) >> currency

        then:
        def result = service.resolve(symbol)
        result == currency
    }

    def "resolve - should return null when currency not found"() {
        given:
        def symbol = "USD"

        when:
        currencyAddService.findBySymbol(symbol) >> null

        then:
        def result = service.resolve(symbol)
        result == null
    }
}