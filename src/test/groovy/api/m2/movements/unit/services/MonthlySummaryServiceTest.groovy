package api.m2.movements.unit.services

import api.m2.movements.entities.User
import api.m2.movements.enums.MovementType
import api.m2.movements.records.balance.MonthlySummaryRecord
import api.m2.movements.repositories.MovementRepository
import api.m2.movements.services.balance.MonthlySummaryService
import api.m2.movements.services.user.UserService
import spock.lang.Specification
import spock.lang.Unroll

class MonthlySummaryServiceTest extends Specification {

    MovementRepository movementRepository = Mock()
    UserService userService = Mock()

    MonthlySummaryService service

    def user = Stub(User) {
        getEmail() >> "user@test.com"
    }

    def setup() {
        service = new MonthlySummaryService(movementRepository, userService)
        userService.getAuthenticatedUser() >> user
    }

    // ── getSummary - happy path ────────────────────────────────────────────────

    def "getSummary - should return correct totals and difference for given year and month"() {
        given:
        movementRepository.getTotalByTypeAndMonth("user@test.com", 2025, 4, MovementType.INGRESO.name()) >> new BigDecimal("150000.00")
        movementRepository.getTotalByTypeAndMonth("user@test.com", 2025, 4, MovementType.DEBITO.name()) >> new BigDecimal("87500.00")
        movementRepository.getTopCategoryByMonth("user@test.com", 2025, 4) >> Optional.of("HOGAR")
        // mes anterior: marzo 2025
        movementRepository.getTotalByTypeAndMonth("user@test.com", 2025, 3, MovementType.INGRESO.name()) >> new BigDecimal("140000.00")
        movementRepository.getTotalByTypeAndMonth("user@test.com", 2025, 3, MovementType.DEBITO.name()) >> new BigDecimal("95000.00")

        when:
        def result = service.getSummary(2025, 4)

        then:
        result.year() == 2025
        result.month() == 4
        result.totalIngresado() == new BigDecimal("150000.00")
        result.totalGastado() == new BigDecimal("87500.00")
        result.diferencia() == new BigDecimal("62500.00")
        result.categoriaConMayorGasto() == "HOGAR"
    }

    def "getSummary - should calculate comparacion vs mes anterior correctly"() {
        given:
        movementRepository.getTotalByTypeAndMonth("user@test.com", 2025, 4, MovementType.INGRESO.name()) >> new BigDecimal("150000.00")
        movementRepository.getTotalByTypeAndMonth("user@test.com", 2025, 4, MovementType.DEBITO.name()) >> new BigDecimal("87500.00")
        movementRepository.getTopCategoryByMonth("user@test.com", 2025, 4) >> Optional.empty()
        movementRepository.getTotalByTypeAndMonth("user@test.com", 2025, 3, MovementType.INGRESO.name()) >> new BigDecimal("140000.00")
        movementRepository.getTotalByTypeAndMonth("user@test.com", 2025, 3, MovementType.DEBITO.name()) >> new BigDecimal("95000.00")

        when:
        def result = service.getSummary(2025, 4)
        def comparacion = result.comparacionVsMesAnterior()

        then:
        comparacion.totalIngresadoMesAnterior() == new BigDecimal("140000.00")
        comparacion.totalGastadoMesAnterior() == new BigDecimal("95000.00")
        // gastoActual - gastoAnterior = 87500 - 95000 = -7500 (gastó menos que el mes anterior)
        comparacion.diferenciaGasto() == new BigDecimal("-7500.00")
        // ingresoActual - ingresoAnterior = 150000 - 140000 = 10000
        comparacion.diferenciaIngreso() == new BigDecimal("10000.00")
    }

    def "getSummary - should return null categoriaConMayorGasto when no movements exist"() {
        given:
        movementRepository.getTotalByTypeAndMonth(*_) >> BigDecimal.ZERO
        movementRepository.getTopCategoryByMonth("user@test.com", 2025, 4) >> Optional.empty()

        when:
        def result = service.getSummary(2025, 4)

        then:
        result.categoriaConMayorGasto() == null
        result.totalIngresado() == BigDecimal.ZERO
        result.totalGastado() == BigDecimal.ZERO
        result.diferencia() == BigDecimal.ZERO
    }

    def "getSummary - should set diferencia as ingresado minus gastado"() {
        given:
        movementRepository.getTotalByTypeAndMonth("user@test.com", 2025, 6, MovementType.INGRESO.name()) >> new BigDecimal("5000.00")
        movementRepository.getTotalByTypeAndMonth("user@test.com", 2025, 6, MovementType.DEBITO.name()) >> new BigDecimal("8000.00")
        movementRepository.getTopCategoryByMonth(*_) >> Optional.empty()
        movementRepository.getTotalByTypeAndMonth("user@test.com", 2025, 5, MovementType.INGRESO.name()) >> BigDecimal.ZERO
        movementRepository.getTotalByTypeAndMonth("user@test.com", 2025, 5, MovementType.DEBITO.name()) >> BigDecimal.ZERO

        when:
        def result = service.getSummary(2025, 6)

        then: "diferencia negativa cuando gastos > ingresos"
        result.diferencia() == new BigDecimal("-3000.00")
    }

    // ── enero: mes anterior es diciembre del año previo ────────────────────────

    def "getSummary - should use December of previous year when month is January"() {
        given:
        movementRepository.getTotalByTypeAndMonth("user@test.com", 2025, 1, MovementType.INGRESO.name()) >> new BigDecimal("10000.00")
        movementRepository.getTotalByTypeAndMonth("user@test.com", 2025, 1, MovementType.DEBITO.name()) >> new BigDecimal("6000.00")
        movementRepository.getTopCategoryByMonth(*_) >> Optional.empty()

        when:
        service.getSummary(2025, 1)

        then: "mes anterior debe ser diciembre 2024 (año anterior)"
        1 * movementRepository.getTotalByTypeAndMonth("user@test.com", 2024, 12, MovementType.INGRESO.name()) >> BigDecimal.ZERO
        1 * movementRepository.getTotalByTypeAndMonth("user@test.com", 2024, 12, MovementType.DEBITO.name()) >> BigDecimal.ZERO
    }

    // ── autenticación ──────────────────────────────────────────────────────────

    def "getSummary - should call userService exactly once"() {
        given:
        movementRepository.getTotalByTypeAndMonth(*_) >> BigDecimal.ZERO
        movementRepository.getTopCategoryByMonth(*_) >> Optional.empty()

        when:
        service.getSummary(2025, 4)

        then:
        1 * userService.getAuthenticatedUser() >> user
    }

    // ── @Unroll: diferentes year/month ────────────────────────────────────────

    @Unroll
    def "getSummary - should return a non-null result for year=#year month=#month"() {
        given:
        movementRepository.getTotalByTypeAndMonth(*_) >> new BigDecimal("1000.00")
        movementRepository.getTopCategoryByMonth(*_) >> Optional.of("TRANSPORTE")

        when:
        MonthlySummaryRecord result = service.getSummary(year, month)

        then:
        result != null
        result.year() == year
        result.month() == month

        where:
        year | month
        2024 | 1
        2025 | 6
        2025 | 12
        2026 | 3
    }
}
