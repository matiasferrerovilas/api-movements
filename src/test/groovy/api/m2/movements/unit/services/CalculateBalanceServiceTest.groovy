package api.m2.movements.unit.services

import api.m2.movements.entities.User
import api.m2.movements.enums.BalanceEnum
import api.m2.movements.enums.MovementType
import api.m2.movements.mappers.BalanceEvolutionMapper
import api.m2.movements.projections.MonthlyEvolutionProjection
import api.m2.movements.records.balance.*
import api.m2.movements.repositories.CurrencyRepository
import api.m2.movements.repositories.MovementRepository
import api.m2.movements.services.balance.CalculateBalanceService
import api.m2.movements.services.user.UserService
import api.m2.movements.services.workspaces.WorkspaceContextService
import org.mapstruct.factory.Mappers
import spock.lang.Specification
import spock.lang.Unroll

import java.time.LocalDate

class CalculateBalanceServiceTest extends Specification {

    MovementRepository movementRepository = Mock()
    UserService userService = Mock()
    CurrencyRepository currencyRepository = Mock()
    BalanceEvolutionMapper balanceEvolutionMapper = Mappers.getMapper(BalanceEvolutionMapper)
    WorkspaceContextService workspaceContextService = Mock()

    CalculateBalanceService service

    def user = Stub(User) {
        getEmail() >> "user@test.com"
    }

    def setup() {
        service = new CalculateBalanceService(
                movementRepository,
                userService,
                currencyRepository,
                balanceEvolutionMapper,
                workspaceContextService
        )
        // stub de usuario en todos los tests
        userService.getAuthenticatedUser() >> user
        workspaceContextService.getActiveWorkspaceId() >> 1L
    }

    // ── getBalance ─────────────────────────────────────────────────────────────

    def "getBalance - should return ingreso and gasto correctly"() {
        given:
        def filter = new BalanceFilterRecord(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                ["EUR"]
        )
        currencyRepository.findAllBySymbol(["EUR"]) >> []

        movementRepository.getBalanceByFilters(
                _ as LocalDate, _ as LocalDate, "user@test.com",
                [MovementType.INGRESO.toString()],
                _ as List<Integer>, _ as List
        ) >> new BigDecimal("1000")

        movementRepository.getBalanceByFilters(
                _ as LocalDate, _ as LocalDate, "user@test.com",
                [MovementType.DEBITO.toString()],
                _ as List<Integer>, _ as List
        ) >> new BigDecimal("400")

        when:
        def result = service.getBalance(filter)

        then:
        result[BalanceEnum.INGRESO] == new BigDecimal("1000")
        result[BalanceEnum.GASTO] == new BigDecimal("400")
        result.size() == 2
    }

    def "getBalance - should handle zero values for both ingreso and gasto"() {
        given:
        def filter = new BalanceFilterRecord(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                ["USD"]
        )
        currencyRepository.findAllBySymbol(["USD"]) >> []
        movementRepository.getBalanceByFilters(*_) >> BigDecimal.ZERO

        when:
        def result = service.getBalance(filter)

        then:
        result[BalanceEnum.INGRESO] == BigDecimal.ZERO
        result[BalanceEnum.GASTO] == BigDecimal.ZERO
    }

    def "getBalance - should call repository with correct email from authenticated user"() {
        given:
        def filter = new BalanceFilterRecord(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 30),
                ["ARS"]
        )
        currencyRepository.findAllBySymbol(_) >> []
        movementRepository.getBalanceByFilters(*_) >> BigDecimal.ZERO

        when:
        service.getBalance(filter)

        then:
        // verifica que el email correcto se pasa al repositorio
        2 * movementRepository.getBalanceByFilters(
                _ as LocalDate, _ as LocalDate,
                "user@test.com",
                _ as List, _ as List, _ as List
        )
    }

    @Unroll
    def "getBalance - should handle multiple currencies: #currencies"() {
        given:
        def filter = new BalanceFilterRecord(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                currencies
        )
        currencyRepository.findAllBySymbol(currencies) >> []
        movementRepository.getBalanceByFilters(*_) >> new BigDecimal("500")

        when:
        def result = service.getBalance(filter)

        then:
        result.containsKey(BalanceEnum.INGRESO)
        result.containsKey(BalanceEnum.GASTO)

        where:
        currencies          | _
        ["EUR"]             | _
        ["USD", "ARS"]      | _
        ["EUR", "USD", "CHF"] | _
        []                  | _
    }

    // ── getBalanceWithCategoryByYear ───────────────────────────────────────────

    def "getBalanceWithCategoryByYear - should delegate to repository with correct params"() {
        given:
        def filter = new BalanceFilterRecord(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                ["EUR"]
        )
        def expectedResult = [
                new BalanceByCategoryRecord("Food", 2026, 3, "EUR", new BigDecimal("200"))
        ] as Set

        movementRepository.getBalanceWithCategoryByYear(
                2026, 3, [1], ["EUR"], "user@test.com"
        ) >> expectedResult

        when:
        def result = service.getBalanceWithCategoryByYear(filter)

        then:
        result == expectedResult
    }

    def "getBalanceWithCategoryByYear - should derive year and month from startDate (not endDate)"() {
        given: "startDate and endDate are in different months to verify which one is used"
        def filter = new BalanceFilterRecord(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 9, 30),
                ["EUR"]
        )
        movementRepository.getBalanceWithCategoryByYear(*_) >> ([] as Set)

        when:
        service.getBalanceWithCategoryByYear(filter)

        then: "year and month must come from startDate (2026, 3) — NOT endDate month (9)"
        1 * movementRepository.getBalanceWithCategoryByYear(2026, 3, [1], ["EUR"], "user@test.com") >> ([] as Set)
    }

    def "getBalanceWithCategoryByYear - should return empty set when no data"() {
        given:
        def filter = new BalanceFilterRecord(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                []
        )
        movementRepository.getBalanceWithCategoryByYear(*_) >> ([] as Set)

        when:
        def result = service.getBalanceWithCategoryByYear(filter)

        then:
        result.isEmpty()
    }

    // ── getBalanceByYearAndGroup ───────────────────────────────────────────────

    @Unroll
    def "getBalanceByYearAndGroup - should work for year=#year month=#month"() {
        given:
        movementRepository.getBalanceByYearAndGroup(year, month, "user@test.com") >> ([] as Set)

        when:
        def result = service.getBalanceByYearAndGroup(year, month)

        then:
        result != null

        where:
        year | month
        2024 | 1
        2025 | 6
        2026 | 12
    }

    // ── getMonthlyEvolution ────────────────────────────────────────────────────

    def "getMonthlyEvolution - should return 12 months filled for each currency in results"() {
        given: "repository returns data for 2 months in EUR"
        def proj1 = Stub(MonthlyEvolutionProjection) {
            getMonth()          >> 1
            getCurrencySymbol() >> "EUR"
            getTotal()          >> new BigDecimal("100")
        }
        def proj2 = Stub(MonthlyEvolutionProjection) {
            getMonth()          >> 6
            getCurrencySymbol() >> "EUR"
            getTotal()          >> new BigDecimal("200")
        }
        movementRepository.findMonthlyEvolution(2026 as Integer, [1L]) >> [proj1, proj2]

        when:
        def result = service.getMonthlyEvolution(2026)

        then: "mapper fills all 12 months for EUR, missing months get BigDecimal.ZERO"
        result.size() == 12
        result.find { it.month() == 1 }.total()  == new BigDecimal("100")
        result.find { it.month() == 6 }.total()  == new BigDecimal("200")
        result.find { it.month() == 3 }.total()  == BigDecimal.ZERO
        result.every { it.currencySymbol() == "EUR" }
    }

    def "getMonthlyEvolution - should return empty list when repository has no data"() {
        given: "repository returns no projections"
        movementRepository.findMonthlyEvolution(2026 as Integer, [1L]) >> []

        when:
        def result = service.getMonthlyEvolution(2026)

        then: "mapper returns empty — no currencies to fill"
        result.isEmpty()
    }

    def "getMonthlyEvolution - should return 24 records for 2 currencies"() {
        given: "repository returns data for EUR and USD"
        def eurProj = Stub(MonthlyEvolutionProjection) {
            getMonth()          >> 3
            getCurrencySymbol() >> "EUR"
            getTotal()          >> new BigDecimal("300")
        }
        def usdProj = Stub(MonthlyEvolutionProjection) {
            getMonth()          >> 3
            getCurrencySymbol() >> "USD"
            getTotal()          >> new BigDecimal("150")
        }
        movementRepository.findMonthlyEvolution(2026 as Integer, [1L]) >> [eurProj, usdProj]

        when:
        def result = service.getMonthlyEvolution(2026)

        then: "12 months per currency = 24 total"
        result.size() == 24
        result.count { it.currencySymbol() == "EUR" } == 12
        result.count { it.currencySymbol() == "USD" } == 12
    }

    def "getMonthlyEvolution - should not call userService (no auth needed)"() {
        given:
        movementRepository.findMonthlyEvolution(_ as Integer, _ as List) >> []

        when:
        service.getMonthlyEvolution(2026)

        then:
        0 * userService.getAuthenticatedUser()
    }
}