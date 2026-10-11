package api.m2.movements.unit.services

import api.m2.movements.helpers.GeneratedLedger
import spock.lang.Specification

class GeneratedLedgerTest extends Specification {

    def "consume - should report false for something that was never generated"() {
        expect:
        !new GeneratedLedger<String>([]).consume("a")
    }

    def "consume - should report true once per existing occurrence"() {
        given:
        def ledger = new GeneratedLedger<String>(["a", "a", "b"])

        expect:
        ledger.consume("a")
        ledger.consume("a")
        !ledger.consume("a")
        ledger.consume("b")
        !ledger.consume("b")
    }
}
