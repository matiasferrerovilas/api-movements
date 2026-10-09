package api.m2.movements.unit.helpers

import api.m2.movements.entities.commons.Currency
import api.m2.movements.enums.MovementType
import api.m2.movements.exceptions.BusinessException
import api.m2.movements.helpers.SantanderPdfExtractorHelper
import api.m2.movements.repositories.CurrencyRepository
import spock.lang.Specification

import java.time.LocalDate

class SantanderPdfExtractorHelperTest extends Specification {

    CurrencyRepository currencyRepository = Stub(CurrencyRepository)
    Currency eur = new Currency(symbol: "EUR", description: "Euro")
    SantanderPdfExtractorHelper parser

    def setup() {
        currencyRepository.findBySymbol("EUR") >> Optional.of(eur)
        parser = new SantanderPdfExtractorHelper(currencyRepository)
    }

    private static String fixture(String name) {
        SantanderPdfExtractorHelperTest.getResourceAsStream("/pdf/santander/${name}").getText("UTF-8")
    }

    def "getBank - should return SANTANDER"() {
        expect:
        parser.getBank() == "SANTANDER"
    }

    def "parse - should read every account movement across pages"() {
        when:
        def result = parser.parse(fixture("cuenta.txt"))

        then:
        result.size() == 8
        result.every { it.currency() == eur }
        result.every { it.installment() == null }
    }

    def "parse - should map account amount sign to DEBITO or INGRESO"() {
        when:
        def result = parser.parse(fixture("cuenta.txt"))

        then:
        result[0].type() == MovementType.DEBITO
        result[0].amountPesos() == new BigDecimal("2.50")
        result[0].date() == LocalDate.of(2026, 10, 8)

        and:
        def bizum = result.find { it.reference().startsWith("Bizum De") }
        bizum.type() == MovementType.INGRESO
        bizum.amountPesos() == new BigDecimal("111.72")
    }

    def "parse - should parse thousands separator in account amounts"() {
        when:
        def result = parser.parse(fixture("cuenta.txt"))

        then:
        result.find { it.reference().startsWith("Eroski") }.amountPesos() == new BigDecimal("1235.01")
    }

    def "parse - should clean card prefixes and suffixes from account concepts"() {
        when:
        def references = parser.parse(fixture("cuenta.txt"))*.reference()

        then:
        references == [
                "Mercadona Bulev, La Coruna Es",
                "Arena Padel, A Coru#a Es",
                "A Coruµa, A Coruna",
                "Bizum De Ana Lopez Concepto Sin Concepto",
                "Recibo O2 Fibra - Telefonica De Espana Sau, Concepto: Fijoxxxxxx000.oct",
                "Eroski Center P, A Coru#a Es",
                "Transferencia Inmediata A Favor De Pedro Martinez Concepto Alquiler Octubre",
                "Dia 8961, La Coruna Es"
        ]
    }

    def "parse - should read only card operations from credit statement"() {
        when:
        def result = parser.parse(fixture("credito.txt"))

        then:
        result*.reference() == [
                "O CABO",
                "WWW.GTT.TO.IT",
                "HM IT0004",
                "CAFFETTERIA ANTONELLI.",
                "VENPAY IVS",
                "CUOTA FRACCIONAR COMPRA VUELING AIRLINE"
        ]
        result.every { it.currency() == eur }
    }

    def "parse - should use operation date and CREDITO type for card purchases"() {
        when:
        def first = parser.parse(fixture("credito.txt"))[1]

        then:
        first.date() == LocalDate.of(2026, 9, 24)
        first.type() == MovementType.CREDITO
        first.amountPesos() == new BigDecimal("14.80")
    }

    def "parse - should map negative card amounts to REINTEGRO and parse thousands"() {
        when:
        def result = parser.parse(fixture("credito.txt"))

        then:
        result.find { it.reference() == "VENPAY IVS" }.type() == MovementType.REINTEGRO
        result.find { it.reference() == "VENPAY IVS" }.amountPesos() == new BigDecimal("3.00")
        result.find { it.reference() == "CAFFETTERIA ANTONELLI." }.amountPesos() == new BigDecimal("1007.00")
    }

    def "parse - should throw BusinessException when document is not from Santander"() {
        when:
        parser.parse("Resumen BBVA\n01-09-26 COMPRA 123456 100,00")

        then:
        thrown(BusinessException)
    }
}
