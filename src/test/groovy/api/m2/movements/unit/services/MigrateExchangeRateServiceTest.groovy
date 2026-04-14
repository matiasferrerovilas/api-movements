package api.m2.movements.unit.services

import api.m2.movements.entities.Currency
import api.m2.movements.entities.Movement
import api.m2.movements.exceptions.ExchangeRateNotFoundException
import api.m2.movements.repositories.MovementRepository
import api.m2.movements.services.currencies.ExchangeRateResolver
import api.m2.movements.services.movements.MigrateExchangeRateService
import spock.lang.Specification

import java.time.LocalDate

class MigrateExchangeRateServiceTest extends Specification {

    MovementRepository movementRepository = Mock(MovementRepository)
    ExchangeRateResolver exchangeRateResolver = Mock(ExchangeRateResolver)

    MigrateExchangeRateService service

    def setup() {
        service = new MigrateExchangeRateService(movementRepository, exchangeRateResolver)
    }

    def "migrateAll - should set exchange rate on all null movements and return count"() {
        given:
        def currency = Stub(Currency) { getSymbol() >> "ARS" }
        def date = LocalDate.of(2024, 3, 10)
        def m1 = Mock(Movement) { getCurrency() >> currency; getDate() >> date }
        def m2 = Mock(Movement) { getCurrency() >> currency; getDate() >> date }

        movementRepository.findAllByExchangeRateIsNull() >> [m1, m2]
        exchangeRateResolver.resolveRate("ARS", date) >> new BigDecimal("900.000000")

        when:
        def result = service.migrateAll()

        then:
        result == 2
        1 * m1.setExchangeRate(new BigDecimal("900.000000"))
        1 * m2.setExchangeRate(new BigDecimal("900.000000"))
        2 * movementRepository.save(_ as Movement)
    }

    def "migrateAll - should return 0 when no movements have null exchange rate"() {
        given:
        movementRepository.findAllByExchangeRateIsNull() >> []

        when:
        def result = service.migrateAll()

        then:
        result == 0
        0 * exchangeRateResolver._
        0 * movementRepository.save(_ as Movement)
    }

    def "migrateAll - should skip movement when ExchangeRateNotFoundException is thrown"() {
        given:
        def currency = Stub(Currency) { getSymbol() >> "XYZ" }
        def date = LocalDate.of(2024, 3, 10)
        def movement = Mock(Movement) { getCurrency() >> currency; getDate() >> date; getId() >> 1L }

        movementRepository.findAllByExchangeRateIsNull() >> [movement]
        exchangeRateResolver.resolveRate("XYZ", date) >> { throw new ExchangeRateNotFoundException("No rate for XYZ") }

        when:
        def result = service.migrateAll()

        then:
        result == 0
        0 * movement.setExchangeRate(_)
        0 * movementRepository.save(_ as Movement)
    }

    def "migrateAll - should continue processing when some movements fail"() {
        given:
        def currencyArs = Stub(Currency) { getSymbol() >> "ARS" }
        def currencyXyz = Stub(Currency) { getSymbol() >> "XYZ" }
        def date = LocalDate.of(2024, 3, 10)
        def m1 = Mock(Movement) { getCurrency() >> currencyArs; getDate() >> date }
        def m2 = Mock(Movement) { getCurrency() >> currencyXyz; getDate() >> date; getId() >> 2L }
        def m3 = Mock(Movement) { getCurrency() >> currencyArs; getDate() >> date }

        movementRepository.findAllByExchangeRateIsNull() >> [m1, m2, m3]
        exchangeRateResolver.resolveRate("ARS", date) >> new BigDecimal("900.000000")
        exchangeRateResolver.resolveRate("XYZ", date) >> { throw new ExchangeRateNotFoundException("No rate for XYZ") }

        when:
        def result = service.migrateAll()

        then:
        result == 2
        1 * m1.setExchangeRate(new BigDecimal("900.000000"))
        0 * m2.setExchangeRate(_)
        1 * m3.setExchangeRate(new BigDecimal("900.000000"))
        2 * movementRepository.save(_ as Movement)
    }
}
