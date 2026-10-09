package api.m2.movements.unit.services

import api.m2.movements.entities.commons.Bank
import api.m2.movements.entities.commons.Currency
import api.m2.movements.entities.movements.Movement
import api.m2.movements.enums.MovementType
import api.m2.movements.repositories.MovementRepository
import api.m2.movements.services.movements.MovementDuplicateFilter
import spock.lang.Specification

import java.time.LocalDate

class MovementDuplicateFilterTest extends Specification {

    MovementRepository movementRepository = Stub(MovementRepository)
    MovementDuplicateFilter filter

    Bank santander = Bank.builder().id(7L).description("SANTANDER").build()
    Currency eur = Currency.builder().id(3L).symbol("EUR").build()

    def setup() {
        filter = new MovementDuplicateFilter(movementRepository)
    }

    def movement(String date, String amount, MovementType type = MovementType.DEBITO, String description = "Mercadona") {
        Movement.builder()
                .date(LocalDate.parse(date))
                .amount(new BigDecimal(amount))
                .type(type)
                .description(description)
                .bank(santander)
                .currency(eur)
                .workspaceId(1L)
                .build()
    }

    def "filterNew - should drop movements already stored even if the description changed"() {
        given:
        movementRepository.findByWorkspaceIdAndDateBetween(1L, LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-08")) >> [
                movement("2026-10-01", "2.5", MovementType.DEBITO, "Super editado a mano")
        ]
        def candidates = [movement("2026-10-01", "2.50"), movement("2026-10-08", "10.00")]

        when:
        def result = filter.filterNew(candidates, 1L)

        then:
        result.size() == 1
        result[0].is(candidates[1])
    }

    def "filterNew - should keep repeated identical lines beyond the ones already stored"() {
        given:
        movementRepository.findByWorkspaceIdAndDateBetween(1L, _ as LocalDate, _ as LocalDate) >> [movement("2026-10-01", "3.00")]
        def candidates = [movement("2026-10-01", "3.00"), movement("2026-10-01", "3.00")]

        when:
        def result = filter.filterNew(candidates, 1L)

        then:
        result.size() == 1
        result[0].is(candidates[1])
    }

    def "filterNew - should not treat a different type or amount as duplicate"() {
        given:
        movementRepository.findByWorkspaceIdAndDateBetween(1L, _ as LocalDate, _ as LocalDate) >> [movement("2026-10-01", "3.00")]
        def candidates = [movement("2026-10-01", "3.00", MovementType.INGRESO), movement("2026-10-01", "3.01")]

        when:
        def result = filter.filterNew(candidates, 1L)

        then:
        result.size() == 2
    }

    def "filterNew - should return empty list without querying when there are no candidates"() {
        expect:
        filter.filterNew([], 1L) == []
    }
}
