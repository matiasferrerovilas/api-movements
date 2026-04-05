package api.m2.movements.unit.services

import api.m2.movements.records.currencies.ExchangeRateRecord
import api.m2.movements.services.currencies.ExchangeRateResolver
import api.m2.movements.services.currencies.ExchangeRateService
import spock.lang.Specification

import java.time.LocalDate

class ExchangeRateResolverTest extends Specification {

    ExchangeRateService exchangeRateService = Mock(ExchangeRateService)

    ExchangeRateResolver service

    def setup() {
        service = new ExchangeRateResolver(exchangeRateService)
    }

    def "resolveRate - should return 1 directly for USD without calling Frankfurter"() {
        when:
        def result = service.resolveRate("USD", LocalDate.now())

        then:
        result == BigDecimal.ONE
        0 * exchangeRateService._
    }

    def "resolveRate - should return rate from Frankfurter for non-USD symbol"() {
        given:
        def date = LocalDate.of(2024, 6, 15)
        def rate = new ExchangeRateRecord(date, "USD", "ARS", new BigDecimal("900.000000"))
        exchangeRateService.getRatesOnDate("USD", "ARS", date) >> [rate]

        when:
        def result = service.resolveRate("ARS", date)

        then:
        result == new BigDecimal("900.000000")
    }

    def "resolveRate - should be case-insensitive for symbol matching"() {
        given:
        def date = LocalDate.of(2024, 6, 15)
        def rate = new ExchangeRateRecord(date, "USD", "EUR", new BigDecimal("0.920000"))
        exchangeRateService.getRatesOnDate("USD", "EUR", date) >> [rate]

        when:
        def result = service.resolveRate("eur", date)

        then:
        result == new BigDecimal("0.920000")
    }

    def "resolveRate - should return null when Frankfurter returns empty list"() {
        given:
        def date = LocalDate.of(2024, 6, 15)
        exchangeRateService.getRatesOnDate("USD", "XYZ", date) >> []

        when:
        def result = service.resolveRate("XYZ", date)

        then:
        result == null
    }

    def "resolveRate - should return null when Frankfurter throws exception"() {
        given:
        def date = LocalDate.of(2024, 6, 15)
        exchangeRateService.getRatesOnDate("USD", "ARS", date) >> { throw new RuntimeException("timeout") }

        when:
        def result = service.resolveRate("ARS", date)

        then:
        result == null
    }
}
