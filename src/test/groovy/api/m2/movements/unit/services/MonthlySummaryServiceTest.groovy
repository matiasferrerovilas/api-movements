package api.m2.movements.unit.services

import api.m2.movements.entities.User
import api.m2.movements.enums.MovementType
import api.m2.movements.records.balance.MonthlySummaryResponse
import api.m2.movements.repositories.MovementRepository
import api.m2.movements.services.balance.MonthlySummaryService
import api.m2.movements.services.balance.MonthlySummarySnapshotService
import api.m2.movements.services.user.UserService
import spock.lang.Specification
import spock.lang.Unroll

class MonthlySummaryServiceTest extends Specification {

    MovementRepository movementRepository = Mock()
    UserService userService = Mock()
    MonthlySummarySnapshotService snapshotService = Mock()

    MonthlySummaryService service

    def user = Stub(User) {
        getEmail() >> "user@test.com"
    }

    def setup() {
        service = new MonthlySummaryService(movementRepository, userService, snapshotService)
        userService.getAuthenticatedUser() >> user
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private void stubCurrencies(List<String> currencies, int year, int month, int prevYear, int prevMonth) {
        movementRepository.findDistinctCurrenciesByMonth("user@test.com", year, month, prevYear, prevMonth) >> currencies
    }

    private void stubTotalByCurrency(int year, int month, String type, String currency, BigDecimal value) {
        movementRepository.getTotalByTypeAndMonth("user@test.com", year, month, type, currency) >> value
    }

    // ── cache-first: snapshot hit ──────────────────────────────────────────────

    def "getSummary - should return snapshot from cache when it exists"() {
        given:
        def cached = new MonthlySummaryResponse(2025, 4, null, [])
        snapshotService.find(user, 2025, 4) >> Optional.of(cached)

        when:
        def result = service.getSummary(2025, 4)

        then:
        result == cached
        0 * movementRepository._
    }

    def "getSummary - should call userService exactly once on cache hit"() {
        given:
        def cached = new MonthlySummaryResponse(2025, 4, null, [])
        snapshotService.find(user, 2025, 4) >> Optional.of(cached)

        when:
        service.getSummary(2025, 4)

        then:
        1 * userService.getAuthenticatedUser() >> user
    }

    // ── cache-first: snapshot miss → calcula on-demand ────────────────────────

    def "getSummary - should compute on-demand when snapshot is absent"() {
        given:
        snapshotService.find(_ as User, *_) >> Optional.empty()
        stubCurrencies([], 2025, 4, 2025, 3)
        movementRepository.getTotalInUsdByTypeAndMonth(*_) >> BigDecimal.ZERO

        when:
        def result = service.getSummary(2025, 4)

        then:
        result != null
        result.year() == 2025
        result.month() == 4
    }

    // ── computeSummary: estructura general ────────────────────────────────────

    def "computeSummary - should return year and month in response"() {
        given:
        stubCurrencies([], 2025, 4, 2025, 3)
        movementRepository.getTotalInUsdByTypeAndMonth(*_) >> BigDecimal.ZERO

        when:
        def result = service.computeSummary("user@test.com", 2025, 4)

        then:
        result.year() == 2025
        result.month() == 4
    }

    def "computeSummary - should return empty porMoneda when no currencies found"() {
        given:
        stubCurrencies([], 2025, 4, 2025, 3)
        movementRepository.getTotalInUsdByTypeAndMonth(*_) >> BigDecimal.ZERO

        when:
        def result = service.computeSummary("user@test.com", 2025, 4)

        then:
        result.porMoneda().isEmpty()
    }

    // ── computeSummary: porMoneda ─────────────────────────────────────────────

    def "computeSummary - should return one entry per currency with correct totals"() {
        given:
        stubCurrencies(["ARS"], 2025, 4, 2025, 3)
        stubTotalByCurrency(2025, 4, MovementType.INGRESO.name(), "ARS", new BigDecimal("150000.00"))
        stubTotalByCurrency(2025, 4, MovementType.DEBITO.name(), "ARS", new BigDecimal("87500.00"))
        movementRepository.getTopCategoryByMonth("user@test.com", 2025, 4, "ARS") >> Optional.of("HOGAR")
        stubTotalByCurrency(2025, 3, MovementType.INGRESO.name(), "ARS", new BigDecimal("140000.00"))
        stubTotalByCurrency(2025, 3, MovementType.DEBITO.name(), "ARS", new BigDecimal("95000.00"))
        movementRepository.getTotalInUsdByTypeAndMonth(*_) >> BigDecimal.ZERO

        when:
        def result = service.computeSummary("user@test.com", 2025, 4)
        def ars = result.porMoneda().find { it.currency() == "ARS" }

        then:
        result.porMoneda().size() == 1
        ars.totalIngresado() == new BigDecimal("150000.00")
        ars.totalGastado() == new BigDecimal("87500.00")
        ars.diferencia() == new BigDecimal("62500.00")
        ars.categoriaConMayorGasto() == "HOGAR"
    }

    def "computeSummary - should return two entries when two currencies exist"() {
        given:
        stubCurrencies(["ARS", "USD"], 2025, 4, 2025, 3)
        movementRepository.getTotalByTypeAndMonth(*_) >> BigDecimal.ZERO
        movementRepository.getTopCategoryByMonth(*_) >> Optional.empty()
        movementRepository.getTotalInUsdByTypeAndMonth(*_) >> BigDecimal.ZERO

        when:
        def result = service.computeSummary("user@test.com", 2025, 4)

        then:
        result.porMoneda().size() == 2
        result.porMoneda().collect { it.currency() }.containsAll(["ARS", "USD"])
    }

    def "computeSummary - should return null categoriaConMayorGasto when no DEBITO movements"() {
        given:
        stubCurrencies(["USD"], 2025, 4, 2025, 3)
        movementRepository.getTotalByTypeAndMonth(*_) >> BigDecimal.ZERO
        movementRepository.getTopCategoryByMonth("user@test.com", 2025, 4, "USD") >> Optional.empty()
        movementRepository.getTotalInUsdByTypeAndMonth(*_) >> BigDecimal.ZERO

        when:
        def result = service.computeSummary("user@test.com", 2025, 4)

        then:
        result.porMoneda().first().categoriaConMayorGasto() == null
    }

    def "computeSummary - should set diferencia negativa when gastado > ingresado"() {
        given:
        stubCurrencies(["ARS"], 2025, 6, 2025, 5)
        stubTotalByCurrency(2025, 6, MovementType.INGRESO.name(), "ARS", new BigDecimal("5000.00"))
        stubTotalByCurrency(2025, 6, MovementType.DEBITO.name(), "ARS", new BigDecimal("8000.00"))
        movementRepository.getTopCategoryByMonth(*_) >> Optional.empty()
        movementRepository.getTotalByTypeAndMonth("user@test.com", 2025, 5, MovementType.INGRESO.name(), "ARS") >> BigDecimal.ZERO
        movementRepository.getTotalByTypeAndMonth("user@test.com", 2025, 5, MovementType.DEBITO.name(), "ARS") >> BigDecimal.ZERO
        movementRepository.getTotalInUsdByTypeAndMonth(*_) >> BigDecimal.ZERO

        when:
        def result = service.computeSummary("user@test.com", 2025, 6)

        then:
        result.porMoneda().first().diferencia() == new BigDecimal("-3000.00")
    }

    // ── computeSummary: comparacionVsMesAnterior ──────────────────────────────

    def "computeSummary - should calculate comparacion vs mes anterior correctly"() {
        given:
        stubCurrencies(["ARS"], 2025, 4, 2025, 3)
        stubTotalByCurrency(2025, 4, MovementType.INGRESO.name(), "ARS", new BigDecimal("150000.00"))
        stubTotalByCurrency(2025, 4, MovementType.DEBITO.name(), "ARS", new BigDecimal("87500.00"))
        movementRepository.getTopCategoryByMonth(*_) >> Optional.empty()
        stubTotalByCurrency(2025, 3, MovementType.INGRESO.name(), "ARS", new BigDecimal("140000.00"))
        stubTotalByCurrency(2025, 3, MovementType.DEBITO.name(), "ARS", new BigDecimal("95000.00"))
        movementRepository.getTotalInUsdByTypeAndMonth(*_) >> BigDecimal.ZERO

        when:
        def comparacion = service.computeSummary("user@test.com", 2025, 4).porMoneda().first().comparacionVsMesAnterior()

        then:
        comparacion.totalIngresadoMesAnterior() == new BigDecimal("140000.00")
        comparacion.totalGastadoMesAnterior() == new BigDecimal("95000.00")
        comparacion.diferenciaGasto() == new BigDecimal("-7500.00")
        comparacion.diferenciaIngreso() == new BigDecimal("10000.00")
    }

    // ── computeSummary: totalUnificadoUSD ─────────────────────────────────────

    def "computeSummary - should return totalUnificadoUSD with converted amounts"() {
        given:
        stubCurrencies([], 2025, 4, 2025, 3)
        movementRepository.getTotalInUsdByTypeAndMonth("user@test.com", 2025, 4, MovementType.INGRESO.name()) >> new BigDecimal("850.50")
        movementRepository.getTotalInUsdByTypeAndMonth("user@test.com", 2025, 4, MovementType.DEBITO.name()) >> new BigDecimal("610.20")
        movementRepository.getTotalInUsdByTypeAndMonth("user@test.com", 2025, 3, MovementType.INGRESO.name()) >> new BigDecimal("780.00")
        movementRepository.getTotalInUsdByTypeAndMonth("user@test.com", 2025, 3, MovementType.DEBITO.name()) >> new BigDecimal("590.00")

        when:
        def usd = service.computeSummary("user@test.com", 2025, 4).totalUnificadoUSD()

        then:
        usd.totalIngresado() == new BigDecimal("850.50")
        usd.totalGastado() == new BigDecimal("610.20")
        usd.diferencia() == new BigDecimal("240.30")
        usd.comparacionVsMesAnterior().totalIngresadoMesAnterior() == new BigDecimal("780.00")
        usd.comparacionVsMesAnterior().totalGastadoMesAnterior() == new BigDecimal("590.00")
        usd.comparacionVsMesAnterior().diferenciaIngreso() == new BigDecimal("70.50")
        usd.comparacionVsMesAnterior().diferenciaGasto() == new BigDecimal("20.20")
    }

    // ── enero: mes anterior es diciembre del año previo ────────────────────────

    def "computeSummary - should use December of previous year when month is January"() {
        given:
        movementRepository.findDistinctCurrenciesByMonth("user@test.com", 2025, 1, 2024, 12) >> []
        movementRepository.getTotalInUsdByTypeAndMonth(*_) >> BigDecimal.ZERO

        when:
        service.computeSummary("user@test.com", 2025, 1)

        then:
        1 * movementRepository.getTotalInUsdByTypeAndMonth("user@test.com", 2024, 12, MovementType.INGRESO.name()) >> BigDecimal.ZERO
        1 * movementRepository.getTotalInUsdByTypeAndMonth("user@test.com", 2024, 12, MovementType.DEBITO.name()) >> BigDecimal.ZERO
    }

    // ── moneda sin movimientos en mes actual aparece igual ────────────────────

    def "computeSummary - should include currency with zeros when only present in previous month"() {
        given: "USD solo tuvo movimientos en marzo, no en abril"
        stubCurrencies(["USD"], 2025, 4, 2025, 3)
        stubTotalByCurrency(2025, 4, MovementType.INGRESO.name(), "USD", BigDecimal.ZERO)
        stubTotalByCurrency(2025, 4, MovementType.DEBITO.name(), "USD", BigDecimal.ZERO)
        movementRepository.getTopCategoryByMonth(*_) >> Optional.empty()
        stubTotalByCurrency(2025, 3, MovementType.INGRESO.name(), "USD", new BigDecimal("200.00"))
        stubTotalByCurrency(2025, 3, MovementType.DEBITO.name(), "USD", new BigDecimal("150.00"))
        movementRepository.getTotalInUsdByTypeAndMonth(*_) >> BigDecimal.ZERO

        when:
        def result = service.computeSummary("user@test.com", 2025, 4)
        def usd = result.porMoneda().find { it.currency() == "USD" }

        then:
        usd != null
        usd.totalIngresado() == BigDecimal.ZERO
        usd.totalGastado() == BigDecimal.ZERO
        usd.comparacionVsMesAnterior().totalIngresadoMesAnterior() == new BigDecimal("200.00")
    }

    // ── getSummary: userService solo se llama una vez ──────────────────────────

    def "getSummary - should call userService exactly once regardless of currency count"() {
        given:
        snapshotService.find(_ as User, *_) >> Optional.empty()
        stubCurrencies(["ARS", "USD", "EUR"], 2025, 4, 2025, 3)
        movementRepository.getTotalByTypeAndMonth(*_) >> BigDecimal.ZERO
        movementRepository.getTopCategoryByMonth(*_) >> Optional.empty()
        movementRepository.getTotalInUsdByTypeAndMonth(*_) >> BigDecimal.ZERO

        when:
        service.getSummary(2025, 4)

        then:
        1 * userService.getAuthenticatedUser() >> user
    }

    // ── @Unroll ────────────────────────────────────────────────────────────────

    @Unroll
    def "computeSummary - should return non-null response for year=#year month=#month"() {
        given:
        movementRepository.findDistinctCurrenciesByMonth(*_) >> []
        movementRepository.getTotalInUsdByTypeAndMonth(*_) >> BigDecimal.ZERO

        when:
        MonthlySummaryResponse result = service.computeSummary("user@test.com", year, month)

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
