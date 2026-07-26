package api.m2.movements.unit.services


import api.m2.movements.records.balance.MonthlySummaryResponse
import api.m2.movements.repositories.MovementRepository
import api.m2.movements.services.balance.MonthlySummaryJob
import api.m2.movements.services.balance.MonthlySummaryService
import api.m2.movements.services.balance.MonthlySummarySnapshotService
import spock.lang.Specification

class MonthlySummaryJobTest extends Specification {

    MovementRepository movementRepository = Mock()
    MonthlySummaryService monthlySummaryService = Mock()
    MonthlySummarySnapshotService snapshotService = Mock()

    MonthlySummaryJob job

    def setup() {
        job = new MonthlySummaryJob(movementRepository, monthlySummaryService, snapshotService)
    }

    // ── lista vacía: no se invoca ningún servicio de cálculo ──────────────────

    def "generateMonthlySnapshots - should not call computeSummary or save when workspace list is empty"() {
        given:
        movementRepository.findDistinctWorkspaceIds() >> []

        when:
        job.generateMonthlySnapshots()

        then:
        0 * monthlySummaryService.computeSummary(*_)
        0 * snapshotService.save(*_)
    }

    // ── un workspace: se llama computeSummary y save exactamente una vez ──────

    def "generateMonthlySnapshots - should call computeSummary and save once for a single workspace"() {
        given:
        def workspaceId = 1L
        def summary = Stub(MonthlySummaryResponse)
        movementRepository.findDistinctWorkspaceIds() >> [workspaceId]
        monthlySummaryService.computeSummary(workspaceId, _ as Integer, _ as Integer) >> summary

        when:
        job.generateMonthlySnapshots()

        then:
        1 * monthlySummaryService.computeSummary(workspaceId, _ as Integer, _ as Integer) >> summary
        1 * snapshotService.save(workspaceId, _ as Integer, _ as Integer, summary)
    }

    // ── varios workspaces: se llama una vez por cada uno ──────────────────────

    def "generateMonthlySnapshots - should call computeSummary and save once per workspace"() {
        given:
        def workspaceIds = [1L, 2L, 3L]
        movementRepository.findDistinctWorkspaceIds() >> workspaceIds
        monthlySummaryService.computeSummary(_ as Long, _ as Integer, _ as Integer) >> Stub(MonthlySummaryResponse)

        when:
        job.generateMonthlySnapshots()

        then:
        workspaceIds.each { id ->
            1 * monthlySummaryService.computeSummary(id, _ as Integer, _ as Integer) >> Stub(MonthlySummaryResponse)
            1 * snapshotService.save(id, _ as Integer, _ as Integer, _ as MonthlySummaryResponse)
        }
    }

    // ── movementRepository solo se llama una vez ─────────────────────────────

    def "generateMonthlySnapshots - should call findDistinctWorkspaceIds exactly once"() {
        given:
        def workspaceId = 1L
        movementRepository.findDistinctWorkspaceIds() >> [workspaceId]
        monthlySummaryService.computeSummary(*_) >> Stub(MonthlySummaryResponse)

        when:
        job.generateMonthlySnapshots()

        then:
        1 * movementRepository.findDistinctWorkspaceIds() >> [workspaceId]
    }

    // ── el año y mes pasados a computeSummary y save son consistentes ─────────

    def "generateMonthlySnapshots - should pass the same year and month to computeSummary and save"() {
        given:
        def workspaceId = 1L
        def summary = Stub(MonthlySummaryResponse)
        movementRepository.findDistinctWorkspaceIds() >> [workspaceId]
        int capturedYear
        int capturedMonth

        when:
        job.generateMonthlySnapshots()

        then:
        1 * monthlySummaryService.computeSummary(workspaceId, _ as Integer, _ as Integer) >> { Long id, Integer y, Integer m ->
            capturedYear = y
            capturedMonth = m
            summary
        }
        1 * snapshotService.save(workspaceId, { it == capturedYear } as Integer, { it == capturedMonth } as Integer, summary)
    }
}
