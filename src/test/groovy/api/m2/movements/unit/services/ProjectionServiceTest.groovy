package api.m2.movements.unit.services

import api.m2.movements.clients.identity.response.UserMe
import api.m2.movements.enums.MovementType
import api.m2.movements.repositories.MovementRepository
import api.m2.movements.services.projections.ProjectionService
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

    // "Ahora" fijo en 2025-06-15 → último mes cerrado = 2025-05
    Clock clock = Clock.fixed(Instant.parse("2025-06-15T12:00:00Z"), ZoneOffset.UTC)

    ProjectionService service

    def user = new UserMe(1L, "user@test.com", "User", null, "PERSONAL", null)
    def workspaceId = 10L

    def setup() {
        service = new ProjectionService(movementRepository, workspaceQueryService, userService, clock)
        userService.getMe() >> user
    }

    def "getProjection - should verify membership before returning projection"() {
        given:
        movementRepository.getTotalInUsdByType(workspaceId, _ as String) >> BigDecimal.ZERO
        movementRepository.getTotalInUsdByTypeAndMonth(workspaceId, _ as Integer, _ as Integer, _ as String) >> BigDecimal.ZERO

        when:
        service.getProjection(workspaceId)

        then:
        1 * workspaceQueryService.verifyUserIsMemberOfWorkspace(workspaceId, 1L)
    }

    def "getProjection - should project an increasing balance on a positive net trend"() {
        given: "ahorra neto +1000 USD por mes en los últimos 6 meses cerrados, balance acumulado de 5000"
        movementRepository.getTotalInUsdByType(workspaceId, MovementType.INGRESO.name()) >> new BigDecimal("20000.00")
        movementRepository.getTotalInUsdByType(workspaceId, MovementType.DEBITO.name()) >> new BigDecimal("15000.00")
        movementRepository.getTotalInUsdByTypeAndMonth(workspaceId, _ as Integer, _ as Integer, MovementType.INGRESO.name()) >>
                new BigDecimal("3000.00")
        movementRepository.getTotalInUsdByTypeAndMonth(workspaceId, _ as Integer, _ as Integer, MovementType.DEBITO.name()) >>
                new BigDecimal("2000.00")

        when:
        def result = service.getProjection(workspaceId, 6)

        then:
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
        movementRepository.getTotalInUsdByType(workspaceId, _ as String) >> BigDecimal.ZERO
        movementRepository.getTotalInUsdByTypeAndMonth(workspaceId, _ as Integer, _ as Integer, _ as String) >> BigDecimal.ZERO

        when:
        def result = service.getProjection(workspaceId)

        then:
        result.trailingMonths() == 6
    }
}
