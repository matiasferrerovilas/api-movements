package api.m2.movements.unit.services

import api.m2.movements.entities.Currency
import api.m2.movements.entities.Movement
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
        1 * movementRepository.saveAll([m1, m2])
    }

    def "migrateAll - should return 0 when no movements have null exchange rate"() {
        given:
        movementRepository.findAllByExchangeRateIsNull() >> []

        when:
        def result = service.migrateAll()

        then:
        result == 0
        0 * exchangeRateResolver._
        1 * movementRepository.saveAll([])
    }

    def "migrateAll - should set null when Frankfurter cannot resolve rate"() {
        given:
        def currency = Stub(Currency) { getSymbol() >> "XYZ" }
        def date = LocalDate.of(2024, 3, 10)
        def movement = Mock(Movement) { getCurrency() >> currency; getDate() >> date }

        movementRepository.findAllByExchangeRateIsNull() >> [movement]
        exchangeRateResolver.resolveRate("XYZ", date) >> null

        when:
        service.migrateAll()

        then:
        1 * movement.setExchangeRate(null)
    }
}
