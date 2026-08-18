package api.m2.movements.unit.services

import api.m2.movements.clients.identity.response.UserMe
import api.m2.movements.entities.WorkspaceCurrency
import api.m2.movements.entities.commons.Currency
import api.m2.movements.enums.MovementType
import api.m2.movements.enums.UserSettingKey
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.exceptions.ExchangeRateNotFoundException
import api.m2.movements.records.settings.UserSettingResponse
import api.m2.movements.repositories.MovementRepository
import api.m2.movements.repositories.WorkspaceCurrencyRepository
import api.m2.movements.services.currencies.ExchangeRateResolver
import api.m2.movements.services.projections.ProjectionService
import api.m2.movements.services.settings.UserSettingService
import api.m2.movements.services.user.UserService
import api.m2.movements.services.workspaces.WorkspaceQueryService
import spock.lang.Specification

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ProjectionServiceTest extends Specification {

    MovementRepository movementRepository = Mock()
    WorkspaceQueryService workspaceQueryService = Mock()
    UserService userService = Mock()
    UserSettingService userSettingService = Mock()
    WorkspaceCurrencyRepository workspaceCurrencyRepository = Mock()
    ExchangeRateResolver exchangeRateResolver = Mock()

    // "Ahora" fijo en 2025-06-15 → último mes cerrado = 2025-05
    Clock clock = Clock.fixed(Instant.parse("2025-06-15T12:00:00Z"), ZoneOffset.UTC)

    ProjectionService service

    def user = new UserMe(1L, "user@test.com", "User", null, "PERSONAL", null)
    def workspaceId = 10L

    def setup() {
        service = new ProjectionService(movementRepository, workspaceQueryService, userService,
                userSettingService, workspaceCurrencyRepository, exchangeRateResolver, clock)
        userService.getMe() >> user
        // NOTA: a propósito no se stubea userSettingService.getByKey acá — Spock resuelve
        // interacciones por orden de declaración (la primera que matchea gana, no la más
        // específica), así que un stub acá pisaría cualquier override posterior en el given:
        // de un test individual. Cada test stubea explícitamente lo que necesita.
    }

    // DEFAULT_CURRENCY guarda el id de la fila WorkspaceCurrency (la asociación workspace-moneda),
    // NO el id de Currency directamente — son tablas distintas con ids independientes
    // (ver WorkspaceCurrencyService.toRecord, que usa workspaceCurrency.getId()).
    private static WorkspaceCurrency workspaceCurrencyOf(Long workspaceCurrencyId, String symbol) {
        def currency = Currency.builder().id(99L).symbol(symbol).description(symbol).enabled(true).build()
        return WorkspaceCurrency.builder().id(workspaceCurrencyId).workspaceId(10L).currency(currency).build()
    }

    def "getProjection - should verify membership before returning projection"() {
        given:
        userSettingService.getByKey(UserSettingKey.DEFAULT_CURRENCY) >> { throw new EntityNotFoundException("no default currency") }
        movementRepository.getTotalInUsdByType(workspaceId, _ as String) >> BigDecimal.ZERO
        movementRepository.getTotalInUsdByTypeAndMonth(workspaceId, _ as Integer, _ as Integer, _ as String) >> BigDecimal.ZERO

        when:
        service.getProjection(workspaceId)

        then:
        1 * workspaceQueryService.verifyUserIsMemberOfWorkspace(workspaceId, 1L)
    }

    def "getProjection - should default to USD when the user has no default currency configured"() {
        given:
        userSettingService.getByKey(UserSettingKey.DEFAULT_CURRENCY) >> { throw new EntityNotFoundException("no default currency") }
        movementRepository.getTotalInUsdByType(workspaceId, _ as String) >> BigDecimal.ZERO
        movementRepository.getTotalInUsdByTypeAndMonth(workspaceId, _ as Integer, _ as Integer, _ as String) >> BigDecimal.ZERO

        when:
        def result = service.getProjection(workspaceId)

        then:
        result.currency() == "USD"
        0 * exchangeRateResolver.resolveRate(*_)
    }

    def "getProjection - should project an increasing balance on a positive net trend, in USD"() {
        given: "ahorra neto +1000 USD por mes en los últimos 6 meses cerrados, balance acumulado de 5000"
        userSettingService.getByKey(UserSettingKey.DEFAULT_CURRENCY) >> { throw new EntityNotFoundException("no default currency") }
        movementRepository.getTotalInUsdByType(workspaceId, MovementType.INGRESO.name()) >> new BigDecimal("20000.00")
        movementRepository.getTotalInUsdByType(workspaceId, MovementType.DEBITO.name()) >> new BigDecimal("15000.00")
        movementRepository.getTotalInUsdByTypeAndMonth(workspaceId, _ as Integer, _ as Integer, MovementType.INGRESO.name()) >>
                new BigDecimal("3000.00")
        movementRepository.getTotalInUsdByTypeAndMonth(workspaceId, _ as Integer, _ as Integer, MovementType.DEBITO.name()) >>
                new BigDecimal("2000.00")

        when:
        def result = service.getProjection(workspaceId, 6)

        then:
        result.currency() == "USD"
        result.currentBalance() == new BigDecimal("5000.00")
        result.averageMonthlyNet() == new BigDecimal("1000.00")
        result.trailingMonths() == 6
        result.projectedPoints().size() == 4
        result.projectedPoints().find { it.monthsOut() == 0 }.projectedBalance() == new BigDecimal("5000.00")
        result.projectedPoints().find { it.monthsOut() == 3 }.projectedBalance() == new BigDecimal("8000.00")
        result.projectedPoints().find { it.monthsOut() == 6 }.projectedBalance() == new BigDecimal("11000.00")
        result.projectedPoints().find { it.monthsOut() == 12 }.projectedBalance() == new BigDecimal("17000.00")
    }

    def "getProjection - should project a decreasing balance on a negative net trend"() {
        given: "gasta 500 USD más de lo que ingresa por mes, balance acumulado de 2000"
        userSettingService.getByKey(UserSettingKey.DEFAULT_CURRENCY) >> { throw new EntityNotFoundException("no default currency") }
        movementRepository.getTotalInUsdByType(workspaceId, MovementType.INGRESO.name()) >> new BigDecimal("10000.00")
        movementRepository.getTotalInUsdByType(workspaceId, MovementType.DEBITO.name()) >> new BigDecimal("8000.00")
        movementRepository.getTotalInUsdByTypeAndMonth(workspaceId, _ as Integer, _ as Integer, MovementType.INGRESO.name()) >>
                new BigDecimal("1000.00")
        movementRepository.getTotalInUsdByTypeAndMonth(workspaceId, _ as Integer, _ as Integer, MovementType.DEBITO.name()) >>
                new BigDecimal("1500.00")

        when:
        def result = service.getProjection(workspaceId, 6)

        then:
        result.currentBalance() == new BigDecimal("2000.00")
        result.averageMonthlyNet() == new BigDecimal("-500.00")
        result.projectedPoints().find { it.monthsOut() == 3 }.projectedBalance() == new BigDecimal("500.00")
        result.projectedPoints().find { it.monthsOut() == 6 }.projectedBalance() == new BigDecimal("-1000.00")
    }

    def "getProjection - should not crash and should return a flat projection when there is no history"() {
        given: "workspace nuevo, sin movimientos"
        userSettingService.getByKey(UserSettingKey.DEFAULT_CURRENCY) >> { throw new EntityNotFoundException("no default currency") }
        movementRepository.getTotalInUsdByType(workspaceId, _ as String) >> BigDecimal.ZERO
        movementRepository.getTotalInUsdByTypeAndMonth(workspaceId, _ as Integer, _ as Integer, _ as String) >> BigDecimal.ZERO

        when:
        def result = service.getProjection(workspaceId, 6)

        then:
        noExceptionThrown()
        result.currentBalance() == BigDecimal.ZERO
        result.averageMonthlyNet() == BigDecimal.ZERO
        result.projectedPoints().every { it.projectedBalance() == BigDecimal.ZERO }
    }

    def "getProjection - should default trailingMonths to 6 when not provided"() {
        given:
        userSettingService.getByKey(UserSettingKey.DEFAULT_CURRENCY) >> { throw new EntityNotFoundException("no default currency") }
        movementRepository.getTotalInUsdByType(workspaceId, _ as String) >> BigDecimal.ZERO
        movementRepository.getTotalInUsdByTypeAndMonth(workspaceId, _ as Integer, _ as Integer, _ as String) >> BigDecimal.ZERO

        when:
        def result = service.getProjection(workspaceId)

        then:
        result.trailingMonths() == 6
    }

    def "getProjection - movements already in the user's default currency are summed natively, without any FX conversion"() {
        given: "el usuario tiene EUR como moneda por defecto (workspaceCurrencyId=2), y TODOS sus movimientos ya están en EUR"
        userSettingService.getByKey(UserSettingKey.DEFAULT_CURRENCY) >> new UserSettingResponse(UserSettingKey.DEFAULT_CURRENCY, 2L)
        workspaceCurrencyRepository.findByIdFetchCurrency(2L) >> Optional.of(workspaceCurrencyOf(2L, "EUR"))
        // La tasa se resuelve igual (no se sabe de antemano si hará falta), pero un valor
        // deliberadamente absurdo prueba que nunca se usa: no hay montos en otra moneda que
        // convertir, así que no debe afectar el resultado final.
        exchangeRateResolver.resolveRate("EUR", _) >> new BigDecimal("999")

        movementRepository.getTotalByType(workspaceId, MovementType.INGRESO.name(), "EUR") >> new BigDecimal("20000.00")
        movementRepository.getTotalByType(workspaceId, MovementType.DEBITO.name(), "EUR") >> new BigDecimal("15000.00")
        movementRepository.getTotalInUsdByTypeExcludingCurrency(workspaceId, _ as String, "EUR") >> BigDecimal.ZERO

        movementRepository.getTotalByTypeAndMonth(workspaceId, _ as Integer, _ as Integer, _ as String, "EUR") >> new BigDecimal("1000.00")
        movementRepository.getTotalInUsdByTypeAndMonthExcludingCurrency(workspaceId, _ as Integer, _ as Integer, _ as String, "EUR") >> BigDecimal.ZERO

        when:
        def result = service.getProjection(workspaceId, 6)

        then: "la tasa (aunque disponible) nunca entra en la cuenta, porque no hay montos en otra moneda"
        result.currency() == "EUR"
        result.currentBalance() == new BigDecimal("5000.00")
    }

    def "getProjection - movements in a foreign currency are converted through the USD pivot into the target currency"() {
        given: "el usuario tiene EUR por defecto (workspaceCurrencyId=2), pero sus movimientos están en ARS; hoy 1 USD = 0.90 EUR"
        userSettingService.getByKey(UserSettingKey.DEFAULT_CURRENCY) >> new UserSettingResponse(UserSettingKey.DEFAULT_CURRENCY, 2L)
        workspaceCurrencyRepository.findByIdFetchCurrency(2L) >> Optional.of(workspaceCurrencyOf(2L, "EUR"))
        exchangeRateResolver.resolveRate("EUR", _) >> new BigDecimal("0.90")

        movementRepository.getTotalByType(workspaceId, _ as String, "EUR") >> BigDecimal.ZERO
        movementRepository.getTotalByTypeAndMonth(workspaceId, _ as Integer, _ as Integer, _ as String, "EUR") >> BigDecimal.ZERO
        movementRepository.getTotalInUsdByTypeExcludingCurrency(workspaceId, MovementType.INGRESO.name(), "EUR") >> new BigDecimal("1000.00")
        movementRepository.getTotalInUsdByTypeExcludingCurrency(workspaceId, MovementType.DEBITO.name(), "EUR") >> BigDecimal.ZERO
        movementRepository.getTotalInUsdByTypeAndMonthExcludingCurrency(workspaceId, _ as Integer, _ as Integer, _ as String, "EUR") >> BigDecimal.ZERO

        when:
        def result = service.getProjection(workspaceId, 6)

        then:
        result.currency() == "EUR"
        result.currentBalance() == new BigDecimal("900.00")
    }

    def "getProjection - falls back to USD when the exchange rate for the user's default currency is unavailable"() {
        given: "workspaceCurrencyId=3 apunta a GBP"
        userSettingService.getByKey(UserSettingKey.DEFAULT_CURRENCY) >> new UserSettingResponse(UserSettingKey.DEFAULT_CURRENCY, 3L)
        workspaceCurrencyRepository.findByIdFetchCurrency(3L) >> Optional.of(workspaceCurrencyOf(3L, "GBP"))
        exchangeRateResolver.resolveRate("GBP", _) >> { throw new ExchangeRateNotFoundException("no rate for GBP") }

        movementRepository.getTotalInUsdByType(workspaceId, _ as String) >> BigDecimal.ZERO
        movementRepository.getTotalInUsdByTypeAndMonth(workspaceId, _ as Integer, _ as Integer, _ as String) >> BigDecimal.ZERO

        when:
        def result = service.getProjection(workspaceId)

        then:
        result.currency() == "USD"
        0 * movementRepository.getTotalByType(*_)
    }

    def "getProjection - falls back to USD when the DEFAULT_CURRENCY setting points to a WorkspaceCurrency that no longer exists"() {
        given: "el valor guardado es un id de WorkspaceCurrency huérfano (por ejemplo, se eliminó esa moneda del workspace)"
        userSettingService.getByKey(UserSettingKey.DEFAULT_CURRENCY) >> new UserSettingResponse(UserSettingKey.DEFAULT_CURRENCY, 404L)
        workspaceCurrencyRepository.findByIdFetchCurrency(404L) >> Optional.empty()

        movementRepository.getTotalInUsdByType(workspaceId, _ as String) >> BigDecimal.ZERO
        movementRepository.getTotalInUsdByTypeAndMonth(workspaceId, _ as Integer, _ as Integer, _ as String) >> BigDecimal.ZERO

        when:
        def result = service.getProjection(workspaceId)

        then:
        result.currency() == "USD"
    }
}
