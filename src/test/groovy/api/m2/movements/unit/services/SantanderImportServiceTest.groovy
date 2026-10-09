package api.m2.movements.unit.services

import api.m2.movements.entities.commons.Currency
import api.m2.movements.enums.MovementType
import api.m2.movements.helpers.ParserRegistry
import api.m2.movements.helpers.PdfExtractorHelper
import api.m2.movements.records.categories.CategoryRecord
import api.m2.movements.records.movements.MovementFileToAdd
import api.m2.movements.records.movements.MovementToAdd
import api.m2.movements.records.pdf.ParsedExpense
import api.m2.movements.services.category.CategoryAddService
import api.m2.movements.services.movements.MovementAddService
import api.m2.movements.services.movements.files.strategies.SantanderImportService
import spock.lang.Specification

import java.time.LocalDate

class SantanderImportServiceTest extends Specification {

    MovementAddService movementAddService = Mock(MovementAddService)
    ParserRegistry parserRegistry = Stub(ParserRegistry)
    CategoryAddService categoryAddService = Stub(CategoryAddService)
    PdfExtractorHelper parser = Stub(PdfExtractorHelper)
    SantanderImportService service

    Currency eur = new Currency(symbol: "EUR", description: "Euro")

    def setup() {
        service = new SantanderImportService(movementAddService, parserRegistry, categoryAddService)
        parserRegistry.getParser("SANTANDER") >> parser
        categoryAddService.resolveDefaultCategory(_ as String) >> new CategoryRecord(1L, "SIN_CATEGORIA", true, false, null, null)
    }

    def "match - should match SANTANDER case insensitively and not SANTANDER RIO"() {
        expect:
        service.match("santander")
        !service.match("SANTANDER RIO")
        !service.match("BBVA")
    }

    def "process - should keep the per-line type informed by the parser"() {
        given:
        parser.parse("texto") >> [
                new ParsedExpense(LocalDate.of(2026, 10, 8), "Mercadona", null, null, eur, new BigDecimal("2.50"), null, MovementType.DEBITO),
                new ParsedExpense(LocalDate.of(2026, 10, 7), "Bizum De Ana", null, null, eur, new BigDecimal("111.72"), null, MovementType.INGRESO),
                new ParsedExpense(LocalDate.of(2026, 9, 26), "VENPAY IVS", null, null, eur, new BigDecimal("3.00"), null, MovementType.REINTEGRO)
        ]

        when:
        service.process(new MovementFileToAdd("texto", 1L))

        then:
        1 * movementAddService.saveExpenseAll(_ as List<MovementToAdd>) >> { List args ->
            def movements = args[0] as List<MovementToAdd>
            assert movements*.type() == ["DEBITO", "INGRESO", "REINTEGRO"]
            assert movements*.amount() == [new BigDecimal("2.50"), new BigDecimal("111.72"), new BigDecimal("3.00")]
            assert movements.every { it.currency() == "EUR" && it.bank() == "SANTANDER" }
            assert movements.every { it.cuotaActual() == 0 && it.cuotasTotales() == 0 }
        }
    }

    def "process - should fall back to the strategy type when the parser leaves it empty"() {
        given:
        parser.parse("texto") >> [
                new ParsedExpense(LocalDate.of(2026, 10, 8), "Algo", null, null, eur, new BigDecimal("1.00"), null)
        ]

        when:
        service.process(new MovementFileToAdd("texto", 1L))

        then:
        1 * movementAddService.saveExpenseAll(_ as List<MovementToAdd>) >> { List args ->
            assert (args[0] as List<MovementToAdd>)*.type() == ["DEBITO"]
        }
    }
}
