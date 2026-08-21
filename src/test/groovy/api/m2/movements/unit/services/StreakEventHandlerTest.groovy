package api.m2.movements.unit.services

import api.m2.movements.clients.identity.response.UserBaseRecord
import api.m2.movements.records.categories.CategoryRecord
import api.m2.movements.records.currencies.CurrencyRecord
import api.m2.movements.records.movements.MovementRecord
import api.m2.movements.records.workspaces.WorkspaceBaseRecord
import api.m2.movements.services.gamification.StreakEventHandler
import api.m2.movements.services.gamification.StreakService
import spock.lang.Specification

import java.time.LocalDate
import java.time.LocalDateTime

class StreakEventHandlerTest extends Specification {

    StreakService streakService = Mock()

    StreakEventHandler handler

    def setup() {
        handler = new StreakEventHandler(streakService)
    }

    def buildMovementRecord(LocalDateTime createdAt) {
        def category = new CategoryRecord(1L, "Comida", true, true, null, null)
        def currency = new CurrencyRecord("ARS", 1L)
        def metadata = new MovementRecord.Metadata(
                new UserBaseRecord("Mati", 2L), new WorkspaceBaseRecord(1L, "Familia"), null, null)
        return new MovementRecord(1L, new BigDecimal("1000"), "Super", LocalDate.of(2026, 8, 20),
                createdAt, null, [category], currency, null, "DEBITO", null, null, metadata)
    }

    def "onMovementAdded - forwards the owner, workspace and created-at date to the streak service"() {
        given:
        def record = buildMovementRecord(LocalDateTime.of(2026, 8, 20, 14, 30))

        when:
        handler.onMovementAdded(record)

        then:
        1 * streakService.recordActivity(2L, 1L, LocalDate.of(2026, 8, 20))
    }
}
