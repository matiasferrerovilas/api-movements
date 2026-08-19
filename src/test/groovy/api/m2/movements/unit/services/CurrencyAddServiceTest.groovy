package api.m2.movements.unit.services

import api.m2.movements.services.currencies.CurrencyAddService
import api.m2.movements.entities.commons.Currency
import api.m2.movements.repositories.CurrencyRepository
import api.m2.movements.exceptions.BusinessException
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.exceptions.ServiceException
import spock.lang.Specification
import spock.lang.Unroll

class CurrencyAddServiceTest extends Specification {

    CurrencyRepository currencyRepository = Mock(CurrencyRepository)

    CurrencyAddService service

    def setup() {
        service = new CurrencyAddService(currencyRepository)
    }

    def "addCurrency - should save new currency when symbol does not exist"() {
        given:
        def symbol = "USD"
        def normalizedSymbol = symbol.trim().toUpperCase()
        currencyRepository.findBySymbol(normalizedSymbol) >> Optional.empty()

        when:
        def result = service.addCurrency(symbol)

        then:
        result != null
        result.symbol == normalizedSymbol
        result.description == normalizedSymbol
        1 * currencyRepository.save(_ as Currency) >> { args ->
            def c = args[0] as Currency
            assert c.symbol == normalizedSymbol
            assert c.description == normalizedSymbol
            c // return the saved currency
        }
    }

    def "addCurrency - should return existing currency when symbol exists"() {
        given:
        def symbol = "EUR"
        def normalizedSymbol = symbol.trim().toUpperCase()
        def existingCurrency = Stub(Currency) {
            getSymbol() >> normalizedSymbol
            getDescription() >> normalizedSymbol
        }
        currencyRepository.findBySymbol(normalizedSymbol) >> Optional.of(existingCurrency)

        when:
        def result = service.addCurrency(symbol)

        then:
        result == existingCurrency
        0 * currencyRepository.save(_)
    }

    def "addCurrency(symbol, description) - should save new currency when symbol does not exist"() {
        given:
        def symbol = "BTC"
        def description = "Bitcoin"
        def normalizedSymbol = symbol.trim().toUpperCase()
        currencyRepository.findBySymbol(normalizedSymbol) >> Optional.empty()

        when:
        def result = service.addCurrency(symbol, description)

        then:
        1 * currencyRepository.save(_ as Currency) >> { args ->
            def c = args[0] as Currency
            assert c.symbol == normalizedSymbol
            assert c.description == description
            c
        }
    }

    def "addCurrency(symbol, description) - should return existing currency when symbol exists"() {
        given:
        def symbol = "ARS"
        def normalizedSymbol = symbol.trim().toUpperCase()
        def existingCurrency = Stub(Currency) { getSymbol() >> normalizedSymbol }
        currencyRepository.findBySymbol(normalizedSymbol) >> Optional.of(existingCurrency)

        when:
        def result = service.addCurrency(symbol, "Peso Argentino")

        then:
        result == existingCurrency
        0 * currencyRepository.save(_)
    }

    def "addCurrency(symbol, description) - should throw BusinessException when symbol is blank"() {
        when:
        service.addCurrency(" ", "Description")

        then:
        thrown(BusinessException)
    }

    def "addCurrency(symbol, description) - should throw BusinessException when description is blank"() {
        when:
        service.addCurrency("BTC", " ")

        then:
        thrown(BusinessException)
    }

    def "findBySymbol - should return currency when symbol exists"() {
        given:
        def symbol = "GBP"
        def normalizedSymbol = symbol.trim().toUpperCase()
        def existingCurrency = Stub(Currency) {
            getSymbol() >> normalizedSymbol
        }
        currencyRepository.findBySymbol(normalizedSymbol) >> Optional.of(existingCurrency)

        when:
        def result = service.findBySymbol(symbol)

        then:
        result == existingCurrency
    }

    def "findBySymbol - should throw EntityNotFoundException when symbol does not exist"() {
        given:
        def symbol = "JPY"
        def normalizedSymbol = symbol.trim().toUpperCase()
        currencyRepository.findBySymbol(normalizedSymbol) >> Optional.empty()

        when:
        service.findBySymbol(symbol)

        then:
        thrown(EntityNotFoundException)
    }

    def "getDefaultCurrency - should return the currency flagged as default"() {
        given:
        def usd = Stub(Currency) { getSymbol() >> "USD" }
        currencyRepository.findAllByIsDefaultTrue() >> [usd]

        when:
        def result = service.getDefaultCurrency()

        then:
        result == usd
    }

    def "getDefaultCurrency - should throw ServiceException when no currency is flagged as default"() {
        given:
        currencyRepository.findAllByIsDefaultTrue() >> []

        when:
        service.getDefaultCurrency()

        then:
        thrown(ServiceException)
    }

    def "getDefaultCurrency - should return the first one when more than one currency is flagged as default"() {
        given:
        def usd = Stub(Currency) { getSymbol() >> "USD" }
        def eur = Stub(Currency) { getSymbol() >> "EUR" }
        currencyRepository.findAllByIsDefaultTrue() >> [usd, eur]

        when:
        def result = service.getDefaultCurrency()

        then:
        result == usd
    }

    @Unroll
    def "findBySymbol - should normalize symbol to uppercase"() {
        given:
        def symbol = input
        def normalizedSymbol = symbol.trim().toUpperCase()
        def existingCurrency = Stub(Currency) {
            getSymbol() >> normalizedSymbol
        }
        currencyRepository.findBySymbol(normalizedSymbol) >> Optional.of(existingCurrency)

        when:
        def result = service.findBySymbol(symbol)

        then:
        result == existingCurrency

        where:
        input << ["usd", "USD", " Usd ", "usD"]
    }
}