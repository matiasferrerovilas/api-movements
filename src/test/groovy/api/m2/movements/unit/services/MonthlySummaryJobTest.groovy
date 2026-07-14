package api.m2.movements.unit.services


import api.m2.movements.records.balance.MonthlySummaryResponse
import api.m2.movements.services.balance.MonthlySummaryJob
import api.m2.movements.services.balance.MonthlySummaryService
import api.m2.movements.services.balance.MonthlySummarySnapshotService
import api.m2.movements.services.user.UserService
import spock.lang.Specification

class MonthlySummaryJobTest extends Specification {

    UserService userService = Mock()
    MonthlySummaryService monthlySummaryService = Mock()
    MonthlySummarySnapshotService snapshotService = Mock()

    MonthlySummaryJob job

    def setup() {
        job = new MonthlySummaryJob(userService, monthlySummaryService, snapshotService)
    }

    // ── lista vacía: no se invoca ningún servicio de cálculo ──────────────────

    def "generateMonthlySnapshots - should not call computeSummary or save when user list is empty"() {
        given:
        userService.getUsersWithMonthlySnapshotEnabled() >> []

        when:
        job.generateMonthlySnapshots()

        then:
        0 * monthlySummaryService.computeSummary(*_)
        0 * snapshotService.save(*_)
    }

    // ── un usuario: se llama computeSummary y save exactamente una vez ────────

    def "generateMonthlySnapshots - should call computeSummary and save once for a single user"() {
        given:
        def userId = 1L
        def summary = Stub(MonthlySummaryResponse)
        userService.getUsersWithMonthlySnapshotEnabled() >> [userId]
        monthlySummaryService.computeSummary(userId, _ as Integer, _ as Integer) >> summary

        when:
        job.generateMonthlySnapshots()

        then:
        1 * monthlySummaryService.computeSummary(userId, _ as Integer, _ as Integer) >> summary
        1 * snapshotService.save(userId, _ as Integer, _ as Integer, summary)
    }

    // ── varios usuarios: se llama una vez por cada uno ────────────────────────

    def "generateMonthlySnapshots - should call computeSummary and save once per user"() {
        given:
        def userIds = [1L, 2L, 3L]
        userService.getUsersWithMonthlySnapshotEnabled() >> userIds
        monthlySummaryService.computeSummary(_ as Long, _ as Integer, _ as Integer) >> Stub(MonthlySummaryResponse)

        when:
        job.generateMonthlySnapshots()

        then:
        userIds.each { id ->
            1 * monthlySummaryService.computeSummary(id, _ as Integer, _ as Integer) >> Stub(MonthlySummaryResponse)
            1 * snapshotService.save(id, _ as Integer, _ as Integer, _ as MonthlySummaryResponse)
        }
    }

    // ── userService solo se llama una vez ─────────────────────────────────────

    def "generateMonthlySnapshots - should call getUsersWithMonthlySnapshotEnabled exactly once"() {
        given:
        def userId = 1L
        userService.getUsersWithMonthlySnapshotEnabled() >> [userId]
        monthlySummaryService.computeSummary(*_) >> Stub(MonthlySummaryResponse)

        when:
        job.generateMonthlySnapshots()

        then:
        1 * userService.getUsersWithMonthlySnapshotEnabled() >> [userId]
    }

    // ── el año y mes pasados a computeSummary y save son consistentes ─────────

    def "generateMonthlySnapshots - should pass the same year and month to computeSummary and save"() {
        given:
        def userId = 1L
        def summary = Stub(MonthlySummaryResponse)
        userService.getUsersWithMonthlySnapshotEnabled() >> [userId]
        int capturedYear
        int capturedMonth

        when:
        job.generateMonthlySnapshots()

        then:
        1 * monthlySummaryService.computeSummary(userId, _ as Integer, _ as Integer) >> { Long id, Integer y, Integer m ->
            capturedYear = y
            capturedMonth = m
            summary
        }
        1 * snapshotService.save(userId, { it == capturedYear } as Integer, { it == capturedMonth } as Integer, summary)
    }
}
