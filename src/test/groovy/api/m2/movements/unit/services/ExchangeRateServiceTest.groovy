package api.m2.movements.unit.services

import api.m2.movements.exceptions.BusinessException
import api.m2.movements.records.accounts.AccountBaseRecord
import api.m2.movements.records.categories.CategoryRecord
import api.m2.movements.records.currencies.CurrencyRecord
import api.m2.movements.records.currencies.ExchangeRateRecord
import api.m2.movements.records.movements.MovementRecord
import api.m2.movements.records.users.UserBaseRecord
import api.m2.movements.services.currencies.ExchangeRateService
import api.m2.movements.services.currencies.FrankfurterClient
import spock.lang.Specification

import java.time.LocalDate
import java.time.LocalDateTime

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

    // --- helpers ---

    private static MovementRecord buildMovementRecord(
            String currencySymbol, BigDecimal amount, BigDecimal exchangeRate) {
        def currency = new CurrencyRecord(currencySymbol, 1L)
        def category = new CategoryRecord(1L, "HOGAR", true, true)
        def owner = new UserBaseRecord("test@test.com", 1L)
        def account = new AccountBaseRecord(1L, "Mi cuenta")
        return new MovementRecord(
                1L,
                amount,
                "Test movement",
                LocalDate.now(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                category,
                currency,
                "GALICIA",
                "DEBITO",
                owner,
                account,
                null,
                null,
                exchangeRate,
                null
        )
    }
}
