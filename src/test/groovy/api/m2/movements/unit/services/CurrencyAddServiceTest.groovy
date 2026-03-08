package api.m2.movements.unit.services

import api.m2.movements.services.currencies.CurrencyAddService
import api.m2.movements.configuration.CacheConfiguration
import api.m2.movements.entities.Currency
import api.m2.movements.mappers.CurrencyMapper
import api.m2.movements.records.currencies.CurrencyRecord
import api.m2.movements.repositories.CurrencyRepository
import jakarta.persistence.EntityNotFoundException
import org.mapstruct.factory.Mappers
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import spock.lang.Specification
import spock.lang.Unroll

class CurrencyAddServiceTest extends Specification {

    CurrencyRepository currencyRepository = Mock(CurrencyRepository)
    CurrencyMapper currencyMapper = Mappers.getMapper(CurrencyMapper)

    CurrencyAddService service

    def setup() {
        service = new CurrencyAddService(currencyRepository, currencyMapper)
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

    def "getAllCurrencies - should return list of currency records"() {
        given:
        def currencies = [
                Stub(Currency) {
                    getSymbol() >> "USD"
                    getId() >> 1L
                },
                Stub(Currency) {
                    getSymbol() >> "EUR"
                    getId() >> 2L
                }
        ]
        currencyRepository.findAllByEnabled(true) >> currencies
        currencyMapper.toRecordList(currencies) >> [
                new CurrencyRecord("USD", 1L),
                new CurrencyRecord("EUR", 2L)
        ]

        when:
        def result = service.getAllCurrencies()

        then:
        result.size() == 2
        result.find { it.symbol == "USD" }.id == 1L
        result.find { it.symbol == "EUR" }.id == 2L
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