package api.m2.movements.unit.helpers

import api.m2.movements.entities.Currency
import api.m2.movements.helpers.SantanderExcelExtractorHelper
import api.m2.movements.repositories.CurrencyRepository
import spock.lang.Specification

import java.time.LocalDate

class SantanderExcelExtractorHelperTest extends Specification {

    CurrencyRepository currencyRepository = Mock()
    SantanderExcelExtractorHelper helper

    def setup() {
        helper = new SantanderExcelExtractorHelper(currencyRepository)

        // Stub EUR currency
        currencyRepository.findBySymbol("EUR") >> Optional.of(
                Currency.builder()
                        .id(1L)
                        .symbol("EUR")
                        .description("Euro")
                        .enabled(true)
                        .build()
        )
    }

    def "getBank - should return SANTANDER"() {
        when:
        def result = helper.getBank()

        then:
        result == "SANTANDER"
    }

    def "parseMoney - should convert Spanish format to BigDecimal"() {
        expect:
        helper.parseMoney(input) == expected

        where:
        input        | expected
        "1.234,56"   | new BigDecimal("1234.56")
        "-1.234,56"  | new BigDecimal("-1234.56")
        "123,45"     | new BigDecimal("123.45")
        "-123,45"    | new BigDecimal("-123.45")
        "10,00"      | new BigDecimal("10.00")
    }

    def "parseMoney - should handle numeric format (dot decimal) from Excel cells"() {
        expect:
        helper.parseMoney(input) == expected

        where:
        input    | expected
        "8.9"    | new BigDecimal("8.9")
        "-8.9"   | new BigDecimal("-8.9")
        "123.45" | new BigDecimal("123.45")
        "-123.45"| new BigDecimal("-123.45")
        "10.0"   | new BigDecimal("10.0")
        "1000"   | new BigDecimal("1000")
        "-1000"  | new BigDecimal("-1000")
    }

    def "parseMoney - should handle edge cases"() {
        expect:
        helper.parseMoney(input) == expected

        where:
        input | expected
        null  | null
        ""    | null
        "  "  | null
    }

    def "parseAmount - should return Optional with parsed value"() {
        when:
        def result = helper.parseAmount("-123,45")

        then:
        result.isPresent()
        result.get() == new BigDecimal("-123.45")
    }

    def "parseAmount - should return empty Optional for invalid input"() {
        expect:
        helper.parseAmount(input).isEmpty()

        where:
        input << [null, "", "  ", "invalid", "abc,def"]
    }

    def "parseDate - should parse dd/MM/yyyy format"() {
        when:
        def result = helper.parseDate("13/04/2026")

        then:
        result.isPresent()
        result.get() == LocalDate.of(2026, 4, 13)
    }

    def "parseDate - should return empty Optional for invalid date"() {
        expect:
        helper.parseDate(input).isEmpty()

        where:
        input << [null, "", "  ", "2026-04-13", "invalid", "32/13/2026"]
    }

    def "extractCurrency - should extract currency from text"() {
        expect:
        helper.extractCurrency(input) == expected

        where:
        input              | expected
        "-123,45 EUR"      | "EUR"
        "100,00 USD"       | "USD"
        "50,00 GBP"        | "GBP"
        "EUR 123,45"       | "EUR"
        "CHF 500,00"       | "CHF"
        "ARS 1.000,00"     | "ARS"
    }

    def "extractCurrency - should return EUR as default"() {
        expect:
        helper.extractCurrency(input) == "EUR"

        where:
        input << [null, "", "  ", "123,45", "sin moneda"]
    }

    def "parse - should handle CSV with 5 columns (FECHA OPERACIÓN, FECHA VALOR, CONCEPTO, IMPORTE, SALDO)"() {
        given:
        def csvContent = """FECHA OPERACIÓN;FECHA VALOR;CONCEPTO;IMPORTE;SALDO
13/04/2026;14/04/2026;Compra en supermercado;-123,45 EUR;500,00 EUR
15/04/2026;15/04/2026;Transferencia recibida;1.000,00 EUR;1.500,00 EUR
""".getBytes("UTF-8")

        when:
        def result = helper.parse(csvContent)

        then:
        result.size() == 2
        result[0].reference() == "Compra en supermercado"
        result[0].amountPesos() == new BigDecimal("-123.45")
        result[0].date() == LocalDate.of(2026, 4, 13)
        result[1].reference() == "Transferencia recibida"
        result[1].amountPesos() == new BigDecimal("1000.00")
        result[1].date() == LocalDate.of(2026, 4, 15)
    }

    def "parse - should skip rows with insufficient columns"() {
        given:
        def csvContent = """FECHA OPERACIÓN;FECHA VALOR;CONCEPTO;IMPORTE;SALDO
13/04/2026;14/04/2026
15/04/2026;15/04/2026;Transferencia;-100,00 EUR;500,00 EUR
""".getBytes("UTF-8")

        when:
        def result = helper.parse(csvContent)

        then:
        result.size() == 1
        result[0].reference() == "Transferencia"
    }

    def "parse - should skip rows with empty required fields"() {
        given:
        def csvContent = """FECHA OPERACIÓN;FECHA VALOR;CONCEPTO;IMPORTE;SALDO
;14/04/2026;Sin fecha;;300,00 EUR
13/04/2026;14/04/2026;;50,00 EUR;250,00 EUR
13/04/2026;14/04/2026;Sin importe;;200,00 EUR
15/04/2026;15/04/2026;Válido;-100,00 EUR;100,00 EUR
""".getBytes("UTF-8")

        when:
        def result = helper.parse(csvContent)

        then:
        result.size() == 1
        result[0].reference() == "Válido"
        result[0].amountPesos() == new BigDecimal("-100.00")
    }

    def "parse - should truncate long descriptions to 60 characters"() {
        given:
        def longDescription = "A" * 100  // 100 caracteres
        def csvContent = """FECHA OPERACIÓN;FECHA VALOR;CONCEPTO;IMPORTE;SALDO
13/04/2026;14/04/2026;${longDescription};-100,00 EUR;500,00 EUR
""".getBytes("UTF-8")

        when:
        def result = helper.parse(csvContent)

        then:
        result.size() == 1
        result[0].reference().length() == 60
        result[0].reference() == "A" * 60
    }
}
