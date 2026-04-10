package api.m2.movements.unit.services

import api.m2.movements.entities.MonthlySummarySnapshot
import api.m2.movements.entities.User
import api.m2.movements.records.balance.MonthlySummaryByCurrencyRecord
import api.m2.movements.records.balance.MonthlySummaryComparisonRecord
import api.m2.movements.records.balance.MonthlySummaryResponse
import api.m2.movements.records.balance.MonthlySummaryUnifiedRecord
import api.m2.movements.repositories.MonthlySummarySnapshotRepository
import api.m2.movements.services.balance.MonthlySummarySnapshotService
import spock.lang.Specification
import tools.jackson.databind.json.JsonMapper

class MonthlySummarySnapshotServiceTest extends Specification {

    MonthlySummarySnapshotRepository snapshotRepository = Mock()
    JsonMapper jsonMapper = JsonMapper.builder().build()

    MonthlySummarySnapshotService service

    def user = Stub(User) {
        getId() >> 1L
    }

    def setup() {
        service = new MonthlySummarySnapshotService(snapshotRepository, jsonMapper)
    }

    private MonthlySummaryResponse buildSummary(int year, int month) {
        def comparison = new MonthlySummaryComparisonRecord(
                new BigDecimal("100.00"),
                new BigDecimal("80.00"),
                new BigDecimal("20.00"),
                new BigDecimal("10.00")
        )
        def unified = new MonthlySummaryUnifiedRecord(
                new BigDecimal("500.00"),
                new BigDecimal("300.00"),
                new BigDecimal("200.00"),
                comparison
        )
        def currency = new MonthlySummaryByCurrencyRecord("ARS",
                new BigDecimal("500.00"),
                new BigDecimal("300.00"),
                new BigDecimal("200.00"),
                "HOGAR",
                comparison
        )
        return new MonthlySummaryResponse(year, month, unified, [currency])
    }

    // ── save: crea snapshot cuando no existe ──────────────────────────────────

    def "save - should create new snapshot when none exists"() {
        given:
        def summary = buildSummary(2025, 4)
        snapshotRepository.findByUserAndYearAndMonth(user, 2025, 4) >> Optional.empty()

        when:
        service.save(user, 2025, 4, summary)

        then:
        1 * snapshotRepository.save(_ as MonthlySummarySnapshot) >> { List args ->
            def s = args[0] as MonthlySummarySnapshot
            assert s.user == user
            assert s.year == 2025
            assert s.month == 4
            assert s.payload != null
        }
    }

    // ── save: upsert actualiza el payload si ya existe ────────────────────────

    def "save - should update existing snapshot payload on upsert"() {
        given:
        def existing = Mock(MonthlySummarySnapshot) {
            getPayload() >> '{"old":"data"}'
        }
        def summary = buildSummary(2025, 4)
        snapshotRepository.findByUserAndYearAndMonth(user, 2025, 4) >> Optional.of(existing)

        when:
        service.save(user, 2025, 4, summary)

        then:
        1 * snapshotRepository.save(_ as MonthlySummarySnapshot)
        1 * existing.setPayload(_ as String)
    }

    // ── find: devuelve empty cuando no existe ─────────────────────────────────

    def "find - should return empty when snapshot does not exist"() {
        given:
        snapshotRepository.findByUserAndYearAndMonth(user, 2025, 4) >> Optional.empty()

        when:
        def result = service.find(user, 2025, 4)

        then:
        result.isEmpty()
    }

    // ── find: deserializa correctamente el payload ────────────────────────────

    def "find - should deserialize payload and return correct response"() {
        given:
        def original = buildSummary(2025, 4)
        def json = jsonMapper.writeValueAsString(original)
        def snapshot = Stub(MonthlySummarySnapshot) {
            getPayload() >> json
        }
        snapshotRepository.findByUserAndYearAndMonth(user, 2025, 4) >> Optional.of(snapshot)

        when:
        def result = service.find(user, 2025, 4)

        then:
        result.isPresent()
        result.get().year() == 2025
        result.get().month() == 4
        result.get().totalUnificadoUSD().totalIngresado() == new BigDecimal("500.00")
        result.get().totalUnificadoUSD().totalGastado() == new BigDecimal("300.00")
        result.get().porMoneda().size() == 1
        result.get().porMoneda().first().currency() == "ARS"
        result.get().porMoneda().first().categoriaConMayorGasto() == "HOGAR"
    }

    // ── find: round-trip serialización/deserialización ────────────────────────

    def "save and find - should round-trip serialize and deserialize correctly"() {
        given:
        def otherUser = Stub(User) { getId() >> 2L }
        def original = buildSummary(2026, 1)
        String capturedPayload = null
        snapshotRepository.findByUserAndYearAndMonth(otherUser, 2026, 1) >> Optional.empty()
        snapshotRepository.save(_ as MonthlySummarySnapshot) >> { List args ->
            capturedPayload = (args[0] as MonthlySummarySnapshot).payload
            return args[0]
        }

        when:
        service.save(otherUser, 2026, 1, original)
        def deserialized = jsonMapper.readValue(capturedPayload, MonthlySummaryResponse)

        then:
        deserialized.year() == 2026
        deserialized.month() == 1
        deserialized.totalUnificadoUSD().diferencia() == new BigDecimal("200.00")
    }
}
