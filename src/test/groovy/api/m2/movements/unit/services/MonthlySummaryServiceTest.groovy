package api.m2.movements.unit.services


import api.m2.movements.entities.WorkspaceCurrency
import api.m2.movements.entities.commons.Currency
import api.m2.movements.enums.MovementType
import api.m2.movements.records.balance.MonthlySummaryResponse
import api.m2.movements.repositories.MovementRepository
import api.m2.movements.repositories.WorkspaceCurrencyRepository
import api.m2.movements.services.balance.MonthlySummaryService
import api.m2.movements.services.balance.MonthlySummarySnapshotService
import api.m2.movements.clients.identity.response.UserMe
import api.m2.movements.services.user.UserService
import api.m2.movements.services.workspaces.WorkspaceQueryService
import spock.lang.Specification
import spock.lang.Unroll

class MonthlySummaryServiceTest extends Specification {

    MovementRepository movementRepository = Mock()
    WorkspaceCurrencyRepository workspaceCurrencyRepository = Mock()
    UserService userService = Mock()
    MonthlySummarySnapshotService snapshotService = Mock()
    WorkspaceQueryService workspaceQueryService = Mock()

    MonthlySummaryService service

    def user = new UserMe(1L, "user@test.com", "User", null, "PERSONAL", new UserMe.Metadata(false, true, [], null))

    def workspaceId = 10L

    def setup() {
        service = new MonthlySummaryService(
                movementRepository, workspaceCurrencyRepository, userService, snapshotService, workspaceQueryService)
        userService.getMe() >> user
    }

    private void stubCurrencies(Long forWorkspaceId, List<String> symbols) {
        def workspaceCurrencies = symbols.collect { symbol ->
            def currency = Stub(Currency) { getSymbol() >> symbol }
            Stub(WorkspaceCurrency) { getCurrency() >> currency }
        }
        workspaceCurrencyRepository.findByWorkspaceId(forWorkspaceId) >> workspaceCurrencies
    }

    private void stubTotalByCurrency(int year, int month, String type, String currency, BigDecimal value) {
        movementRepository.getTotalByTypeAndMonth(1L, year, month, type, currency) >> value
    }

    private void stubGastoByCurrency(int year, int month, String currency, BigDecimal value) {
        movementRepository.getTotalByTypesAndMonth(
                1L, year, month, [MovementType.DEBITO.name(), MovementType.CREDITO.name()], currency) >> value
    }

    private void stubGastoInUsd(int year, int month, BigDecimal value) {
        movementRepository.getTotalInUsdByTypesAndMonth(
                1L, year, month, [MovementType.DEBITO.name(), MovementType.CREDITO.name()]) >> value
    }

    def "getSummary - should return snapshot from cache when it exists"() {
        given:
        def cached = new MonthlySummaryResponse(2025, 4, null, [])
        snapshotService.find(workspaceId, 2025, 4) >> Optional.of(cached)

        when:
        def result = service.getSummary(workspaceId, 2025, 4)

        then:
        result == cached
        0 * movementRepository._
    }

    def "getSummary - should call userService exactly once on cache hit"() {
        given:
        def cached = new MonthlySummaryResponse(2025, 4, null, [])
        snapshotService.find(workspaceId, 2025, 4) >> Optional.of(cached)

        when:
        service.getSummary(workspaceId, 2025, 4)

        then:
        1 * userService.getMe() >> user
    }

    def "getSummary - should verify user membership before returning data"() {
        given:
        def cached = new MonthlySummaryResponse(2025, 4, null, [])
        snapshotService.find(workspaceId, 2025, 4) >> Optional.of(cached)

        when:
        service.getSummary(workspaceId, 2025, 4)

        then:
        1 * workspaceQueryService.verifyUserIsMemberOfWorkspace(workspaceId, 1L)
    }

    def "getSummary - should compute on-demand when snapshot is absent"() {
        given:
        snapshotService.find(_ as Long, *_) >> Optional.empty()
        this.stubCurrencies(workspaceId, [])
        movementRepository.getTotalInUsdByTypeAndMonth(*_) >> BigDecimal.ZERO
        movementRepository.getTotalInUsdByTypesAndMonth(*_) >> BigDecimal.ZERO

        when:
        def result = service.getSummary(workspaceId, 2025, 4)

        then:
        result != null
        result.year() == 2025
        result.month() == 4
    }

    def "computeSummary - should return year and month in response"() {
        given:
        this.stubCurrencies(1L, [])
        movementRepository.getTotalInUsdByTypeAndMonth(*_) >> BigDecimal.ZERO
        movementRepository.getTotalInUsdByTypesAndMonth(*_) >> BigDecimal.ZERO

        when:
        def result = service.computeSummary(1L, 2025, 4)

        then:
        result.year() == 2025
        result.month() == 4
    }

    def "computeSummary - should return empty porMoneda when workspace has no currencies configured"() {
        given:
        this.stubCurrencies(1L, [])
        movementRepository.getTotalInUsdByTypeAndMonth(*_) >> BigDecimal.ZERO
        movementRepository.getTotalInUsdByTypesAndMonth(*_) >> BigDecimal.ZERO

        when:
        def result = service.computeSummary(1L, 2025, 4)

        then:
        result.porMoneda().isEmpty()
    }

    def "computeSummary - should return one entry per currency configured in the workspace"() {
        given:
        this.stubCurrencies(1L, ["ARS"])
        stubTotalByCurrency(2025, 4, MovementType.INGRESO.name(), "ARS", new BigDecimal("150000.00"))
        stubGastoByCurrency(2025, 4, "ARS", new BigDecimal("87500.00"))
        movementRepository.getTopCategoryByMonth(1L, 2025, 4, "ARS") >> Optional.of("HOGAR")
        stubTotalByCurrency(2025, 3, MovementType.INGRESO.name(), "ARS", new BigDecimal("140000.00"))
        stubGastoByCurrency(2025, 3, "ARS", new BigDecimal("95000.00"))
        movementRepository.getTotalInUsdByTypeAndMonth(*_) >> BigDecimal.ZERO
        movementRepository.getTotalInUsdByTypesAndMonth(*_) >> BigDecimal.ZERO

        when:
        def result = service.computeSummary(1L, 2025, 4)
        def ars = result.porMoneda().find { it.currency() == "ARS" }

        then:
        result.porMoneda().size() == 1
        ars.totalIngresado() == new BigDecimal("150000.00")
        ars.totalGastado() == new BigDecimal("87500.00")
        ars.diferencia() == new BigDecimal("62500.00")
        ars.categoriaConMayorGasto() == "HOGAR"
    }

    def "computeSummary - CREDITO movements count as gasto, not just DEBITO"() {
        given: "1000 ingresado y 1000 en compras con tarjeta de crédito (CREDITO); sin DEBITO"
        this.stubCurrencies(1L, ["EUR"])
        stubTotalByCurrency(2025, 4, MovementType.INGRESO.name(), "EUR", new BigDecimal("1000.00"))
        stubGastoByCurrency(2025, 4, "EUR", new BigDecimal("1000.00"))
        movementRepository.getTopCategoryByMonth(*_) >> Optional.empty()
        stubTotalByCurrency(2025, 3, MovementType.INGRESO.name(), "EUR", BigDecimal.ZERO)
        stubGastoByCurrency(2025, 3, "EUR", BigDecimal.ZERO)
        movementRepository.getTotalInUsdByTypeAndMonth(*_) >> BigDecimal.ZERO
        movementRepository.getTotalInUsdByTypesAndMonth(*_) >> BigDecimal.ZERO

        when:
        def eur = service.computeSummary(1L, 2025, 4).porMoneda().first()

        then: "el gasto en CREDITO cancela el ingreso — la diferencia real es cero, no 1000"
        eur.totalGastado() == new BigDecimal("1000.00")
        eur.diferencia() == BigDecimal.ZERO
    }

    def "computeSummary - should return two entries when workspace has two currencies configured"() {
        given:
        this.stubCurrencies(1L, ["ARS", "USD"])
        movementRepository.getTotalByTypeAndMonth(*_) >> BigDecimal.ZERO
        movementRepository.getTotalByTypesAndMonth(*_) >> BigDecimal.ZERO
        movementRepository.getTopCategoryByMonth(*_) >> Optional.empty()
        movementRepository.getTotalInUsdByTypeAndMonth(*_) >> BigDecimal.ZERO
        movementRepository.getTotalInUsdByTypesAndMonth(*_) >> BigDecimal.ZERO

        when:
        def result = service.computeSummary(1L, 2025, 4)

        then:
        result.porMoneda().size() == 2
        result.porMoneda().collect { it.currency() }.containsAll(["ARS", "USD"])
    }

    def "computeSummary - should not include a currency removed from the workspace even with historical movements"() {
        given: "ARS tuvo movimientos el mes anterior pero ya no está configurada en el workspace"
        this.stubCurrencies(1L, ["EUR"])
        movementRepository.getTotalByTypeAndMonth(*_) >> BigDecimal.ZERO
        movementRepository.getTotalByTypesAndMonth(*_) >> BigDecimal.ZERO
        movementRepository.getTopCategoryByMonth(*_) >> Optional.empty()
        movementRepository.getTotalInUsdByTypeAndMonth(*_) >> BigDecimal.ZERO
        movementRepository.getTotalInUsdByTypesAndMonth(*_) >> BigDecimal.ZERO

        when:
        def result = service.computeSummary(1L, 2025, 4)

        then:
        result.porMoneda().size() == 1
        result.porMoneda().collect { it.currency() } == ["EUR"]
        0 * movementRepository.getTotalByTypeAndMonth(1L, _ as Integer, _ as Integer, _ as String, "ARS")
        0 * movementRepository.getTotalByTypesAndMonth(1L, _ as Integer, _ as Integer, _ as List, "ARS")
    }

    def "computeSummary - should return null categoriaConMayorGasto when no DEBITO movements"() {
        given:
        this.stubCurrencies(1L, ["USD"])
        movementRepository.getTotalByTypeAndMonth(*_) >> BigDecimal.ZERO
        movementRepository.getTotalByTypesAndMonth(*_) >> BigDecimal.ZERO
        movementRepository.getTopCategoryByMonth(1L, 2025, 4, "USD") >> Optional.empty()
        movementRepository.getTotalInUsdByTypeAndMonth(*_) >> BigDecimal.ZERO
        movementRepository.getTotalInUsdByTypesAndMonth(*_) >> BigDecimal.ZERO

        when:
        def result = service.computeSummary(1L, 2025, 4)

        then:
        result.porMoneda().first().categoriaConMayorGasto() == null
    }

    def "computeSummary - should set diferencia negativa when gastado > ingresado"() {
        given:
        this.stubCurrencies(1L, ["ARS"])
        stubTotalByCurrency(2025, 6, MovementType.INGRESO.name(), "ARS", new BigDecimal("5000.00"))
        stubGastoByCurrency(2025, 6, "ARS", new BigDecimal("8000.00"))
        movementRepository.getTopCategoryByMonth(*_) >> Optional.empty()
        movementRepository.getTotalByTypeAndMonth(1L, 2025, 5, MovementType.INGRESO.name(), "ARS") >> BigDecimal.ZERO
        stubGastoByCurrency(2025, 5, "ARS", BigDecimal.ZERO)
        movementRepository.getTotalInUsdByTypeAndMonth(*_) >> BigDecimal.ZERO
        movementRepository.getTotalInUsdByTypesAndMonth(*_) >> BigDecimal.ZERO

        when:
        def result = service.computeSummary(1L, 2025, 6)

        then:
        result.porMoneda().first().diferencia() == new BigDecimal("-3000.00")
    }

    def "computeSummary - should calculate comparacion vs mes anterior correctly"() {
        given:
        this.stubCurrencies(1L, ["ARS"])
        stubTotalByCurrency(2025, 4, MovementType.INGRESO.name(), "ARS", new BigDecimal("150000.00"))
        stubGastoByCurrency(2025, 4, "ARS", new BigDecimal("87500.00"))
        movementRepository.getTopCategoryByMonth(*_) >> Optional.empty()
        stubTotalByCurrency(2025, 3, MovementType.INGRESO.name(), "ARS", new BigDecimal("140000.00"))
        stubGastoByCurrency(2025, 3, "ARS", new BigDecimal("95000.00"))
        movementRepository.getTotalInUsdByTypeAndMonth(*_) >> BigDecimal.ZERO
        movementRepository.getTotalInUsdByTypesAndMonth(*_) >> BigDecimal.ZERO

        when:
        def comparacion = service.computeSummary(1L, 2025, 4).porMoneda().first().comparacionVsMesAnterior()

        then:
        comparacion.totalIngresadoMesAnterior() == new BigDecimal("140000.00")
        comparacion.totalGastadoMesAnterior() == new BigDecimal("95000.00")
        comparacion.diferenciaGasto() == new BigDecimal("-7500.00")
        comparacion.diferenciaIngreso() == new BigDecimal("10000.00")
    }

    def "computeSummary - should return totalUnificadoUSD with converted amounts"() {
        given:
        this.stubCurrencies(1L, [])
        movementRepository.getTotalInUsdByTypeAndMonth(1L, 2025, 4, MovementType.INGRESO.name()) >> new BigDecimal("850.50")
        stubGastoInUsd(2025, 4, new BigDecimal("610.20"))
        movementRepository.getTotalInUsdByTypeAndMonth(1L, 2025, 3, MovementType.INGRESO.name()) >> new BigDecimal("780.00")
        stubGastoInUsd(2025, 3, new BigDecimal("590.00"))

        when:
        def usd = service.computeSummary(1L, 2025, 4).totalUnificadoUSD()

        then:
        usd.totalIngresado() == new BigDecimal("850.50")
        usd.totalGastado() == new BigDecimal("610.20")
        usd.diferencia() == new BigDecimal("240.30")
        usd.comparacionVsMesAnterior().totalIngresadoMesAnterior() == new BigDecimal("780.00")
        usd.comparacionVsMesAnterior().totalGastadoMesAnterior() == new BigDecimal("590.00")
        usd.comparacionVsMesAnterior().diferenciaIngreso() == new BigDecimal("70.50")
        usd.comparacionVsMesAnterior().diferenciaGasto() == new BigDecimal("20.20")
    }

    def "computeSummary - should use December of previous year when month is January"() {
        given:
        this.stubCurrencies(1L, [])
        movementRepository.getTotalInUsdByTypeAndMonth(*_) >> BigDecimal.ZERO
        movementRepository.getTotalInUsdByTypesAndMonth(*_) >> BigDecimal.ZERO

        when:
        service.computeSummary(1L, 2025, 1)

        then:
        1 * movementRepository.getTotalInUsdByTypeAndMonth(1L, 2024, 12, MovementType.INGRESO.name()) >> BigDecimal.ZERO
        1 * movementRepository.getTotalInUsdByTypesAndMonth(
                1L, 2024, 12, [MovementType.DEBITO.name(), MovementType.CREDITO.name()]) >> BigDecimal.ZERO
    }

    def "computeSummary - should include configured currency with zeros when it has no movements this month"() {
        given: "USD solo tuvo movimientos en marzo, no en abril, pero sigue configurada en el workspace"
        this.stubCurrencies(1L, ["USD"])
        stubTotalByCurrency(2025, 4, MovementType.INGRESO.name(), "USD", BigDecimal.ZERO)
        stubGastoByCurrency(2025, 4, "USD", BigDecimal.ZERO)
        movementRepository.getTopCategoryByMonth(*_) >> Optional.empty()
        stubTotalByCurrency(2025, 3, MovementType.INGRESO.name(), "USD", new BigDecimal("200.00"))
        stubGastoByCurrency(2025, 3, "USD", new BigDecimal("150.00"))
        movementRepository.getTotalInUsdByTypeAndMonth(*_) >> BigDecimal.ZERO
        movementRepository.getTotalInUsdByTypesAndMonth(*_) >> BigDecimal.ZERO

        when:
        def result = service.computeSummary(1L, 2025, 4)
        def usd = result.porMoneda().find { it.currency() == "USD" }

        then:
        usd != null
        usd.totalIngresado() == BigDecimal.ZERO
        usd.totalGastado() == BigDecimal.ZERO
        usd.comparacionVsMesAnterior().totalIngresadoMesAnterior() == new BigDecimal("200.00")
    }

    def "getSummary - should call userService exactly once regardless of currency count"() {
        given:
        snapshotService.find(_ as Long, *_) >> Optional.empty()
        this.stubCurrencies(workspaceId, ["ARS", "USD", "EUR"])
        movementRepository.getTotalByTypeAndMonth(*_) >> BigDecimal.ZERO
        movementRepository.getTotalByTypesAndMonth(*_) >> BigDecimal.ZERO
        movementRepository.getTopCategoryByMonth(*_) >> Optional.empty()
        movementRepository.getTotalInUsdByTypeAndMonth(*_) >> BigDecimal.ZERO
        movementRepository.getTotalInUsdByTypesAndMonth(*_) >> BigDecimal.ZERO

        when:
        service.getSummary(workspaceId, 2025, 4)

        then:
        1 * userService.getMe() >> user
    }

    @Unroll
    def "computeSummary - should return non-null response for year=#year month=#month"() {
        given:
        this.stubCurrencies(1L, [])
        movementRepository.getTotalInUsdByTypeAndMonth(*_) >> BigDecimal.ZERO
        movementRepository.getTotalInUsdByTypesAndMonth(*_) >> BigDecimal.ZERO

        when:
        MonthlySummaryResponse result = service.computeSummary(1L, year, month)

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
