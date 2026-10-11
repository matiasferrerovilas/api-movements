package api.m2.movements.unit.services

import api.m2.movements.enums.MovementType
import api.m2.movements.services.balance.NetSavings
import spock.lang.Specification

import java.time.YearMonth

class NetSavingsTest extends Specification {

    def "averageMonthly - should subtract refunds from spending and average over the closed months"() {
        given:
        def totals = [
                (MovementType.INGRESO_TYPES)   : new BigDecimal("1000.00"),
                (MovementType.GASTO_TYPES)     : new BigDecimal("600.00"),
                (MovementType.REINTEGRO_TYPES) : new BigDecimal("100.00")
        ]

        when:
        def result = NetSavings.averageMonthly(YearMonth.of(2026, 3), 3, { ym, types -> totals[types] })

        then:
        // por mes: 1000 - (600 - 100) = 500
        result == new BigDecimal("500.00")
    }

    def "averageMonthly - should ask for each of the last N months starting at the last closed one"() {
        given:
        def asked = [] as Set

        when:
        NetSavings.averageMonthly(YearMonth.of(2026, 3), 3, { ym, types -> asked << ym; BigDecimal.ZERO })

        then:
        asked == [YearMonth.of(2026, 3), YearMonth.of(2026, 2), YearMonth.of(2026, 1)] as Set
    }

    def "averageMonthly - should round half up to two decimals"() {
        when:
        def result = NetSavings.averageMonthly(YearMonth.of(2026, 3), 3, { ym, types ->
            types == MovementType.INGRESO_TYPES ? BigDecimal.ONE : BigDecimal.ZERO
        })

        then:
        result == new BigDecimal("1.00")
    }
}
