package api.m2.movements.unit.services

import api.m2.movements.exceptions.BusinessException
import api.m2.movements.records.currencies.ExchangeRateRecord
import api.m2.movements.services.currencies.ExchangeRateService
import api.m2.movements.services.currencies.FrankfurterClient
import spock.lang.Specification

import java.time.LocalDate

class ExchangeRateServiceTest extends Specification {

    FrankfurterClient frankfurterClient = Mock(FrankfurterClient)

    ExchangeRateService service

    def setup() {
        service = new ExchangeRateService(frankfurterClient)
    }

    // --- getRates ---

    def "getRates - should delegate to client and return rates"() {
        given:
        def rate = new ExchangeRateRecord(LocalDate.now(), "USD", "ARS", new BigDecimal("990.00"))
        frankfurterClient.getRates("USD", "ARS") >> [rate]

        when:
        def result = service.getRates("USD", "ARS")

        then:
        result == [rate]
    }

    // --- getRatesOnDate ---

    def "getRatesOnDate - should call client with date as string from and to"() {
        given:
        def date = LocalDate.of(2024, 6, 15)
        def rate = new ExchangeRateRecord(date, "USD", "ARS", new BigDecimal("900.00"))
        frankfurterClient.getRatesOnDate("USD", "ARS", "2024-06-15", "2024-06-15") >> [rate]

        when:
        def result = service.getRatesOnDate("USD", "ARS", date)

        then:
        result == [rate]
        0 * frankfurterClient.getRatesByDateRange(_, _, _, _)
    }

    def "getRatesOnDate - should fall back to last available date when exact date has no data"() {
        given:
        def sunday = LocalDate.of(2026, 6, 21)
        def friday = LocalDate.of(2026, 6, 19)
        def fridayRate = new ExchangeRateRecord(friday, "USD", "EUR", new BigDecimal("0.920000"))
        def thursdayRate = new ExchangeRateRecord(friday.minusDays(1), "USD", "EUR", new BigDecimal("0.910000"))
        frankfurterClient.getRatesOnDate("USD", "EUR", "2026-06-21", "2026-06-21") >> []
        frankfurterClient.getRatesByDateRange("USD", "EUR", sunday.minusDays(7), sunday) >> [thursdayRate, fridayRate]

        when:
        def result = service.getRatesOnDate("USD", "EUR", sunday)

        then:
        result == [fridayRate]
    }

    def "getRatesOnDate - should return empty list when no rates found in 7-day lookback"() {
        given:
        def date = LocalDate.of(2026, 6, 21)
        frankfurterClient.getRatesOnDate("USD", "XYZ", "2026-06-21", "2026-06-21") >> []
        frankfurterClient.getRatesByDateRange("USD", "XYZ", date.minusDays(7), date) >> []

        when:
        def result = service.getRatesOnDate("USD", "XYZ", date)

        then:
        result == []
    }

    // --- convertToUsd ---

    def "convertToUsd - should return amount directly when fromSymbol is USD"() {
        given:
        def amount = new BigDecimal("100.00")

        when:
        def result = service.convertToUsd("USD", amount)

        then:
        result == amount
        0 * frankfurterClient._
    }

    def "convertToUsd - should convert amount using rate from Frankfurter"() {
        given:
        def rate = new ExchangeRateRecord(LocalDate.now(), "USD", "ARS", new BigDecimal("1000.00"))
        frankfurterClient.getRates("USD", "ARS") >> [rate]

        when:
        def result = service.convertToUsd("ARS", new BigDecimal("2500.00"))

        then:
        result == new BigDecimal("2.50")
    }

    def "convertToUsd - should throw BusinessException when symbol is not in Frankfurter response"() {
        given:
        frankfurterClient.getRates("USD", "XYZ") >> []

        when:
        service.convertToUsd("XYZ", new BigDecimal("100.00"))

        then:
        thrown(BusinessException)
    }
}
