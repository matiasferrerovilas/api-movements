package api.m2.movements.unit.helpers

import api.m2.movements.helpers.ParserRegistry
import api.m2.movements.helpers.PdfExtractorHelper
import spock.lang.Specification

class ParserRegistryTest extends Specification {

    def "init - should register parsers by bank name"() {
        given:
        def bbvaParser = Mock(PdfExtractorHelper) {
            getBank() >> "BBVA"
        }
        def galiciaParser = Mock(PdfExtractorHelper) {
            getBank() >> "GALICIA"
        }

        when:
        def registry = new ParserRegistry([bbvaParser, galiciaParser])
        registry.init()

        then:
        noExceptionThrown()
    }

    def "init - should throw IllegalStateException when duplicate parser found"() {
        given:
        def parser1 = Mock(PdfExtractorHelper) {
            getBank() >> "BBVA"
        }
        def parser2 = Mock(PdfExtractorHelper) {
            getBank() >> "BBVA"
        }

        when:
        def registry = new ParserRegistry([parser1, parser2])
        registry.init()

        then:
        def ex = thrown(IllegalStateException)
        ex.message == "Duplicate parser found for bank: BBVA"
    }

    def "getParser - should return parser when bank is registered"() {
        given:
        def bbvaParser = Mock(PdfExtractorHelper) {
            getBank() >> "BBVA"
        }
        def registry = new ParserRegistry([bbvaParser])
        registry.init()

        when:
        def result = registry.getParser("BBVA")

        then:
        result == bbvaParser
    }

    def "getParser - should throw IllegalArgumentException when bank is not registered"() {
        given:
        def bbvaParser = Mock(PdfExtractorHelper) {
            getBank() >> "BBVA"
        }
        def registry = new ParserRegistry([bbvaParser])
        registry.init()

        when:
        registry.getParser("UNKNOWN")

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "No parser registered for bank: UNKNOWN"
    }

    def "init - should handle empty parser list"() {
        when:
        def registry = new ParserRegistry([])
        registry.init()

        then:
        noExceptionThrown()
    }

    def "getParser - should throw when no parsers registered"() {
        given:
        def registry = new ParserRegistry([])
        registry.init()

        when:
        registry.getParser("BBVA")

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "No parser registered for bank: BBVA"
    }
}
