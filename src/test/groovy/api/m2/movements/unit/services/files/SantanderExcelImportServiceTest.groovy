package api.m2.movements.unit.services.files

import api.m2.movements.entities.Currency
import api.m2.movements.enums.MovementType
import api.m2.movements.helpers.SantanderExcelExtractorHelper
import api.m2.movements.records.movements.MovementToAdd
import api.m2.movements.records.pdf.ParsedExpense
import api.m2.movements.services.category.CategoryAddService
import api.m2.movements.services.movements.MovementAddService
import api.m2.movements.services.movements.files.strategies.SantanderExcelImportService
import spock.lang.Specification

import java.time.LocalDate

class SantanderExcelImportServiceTest extends Specification {

    MovementAddService movementAddService = Mock()
    SantanderExcelExtractorHelper extractor = Mock()
    CategoryAddService categoryAddService = Mock()
    SantanderExcelImportService service

    def setup() {
        service = new SantanderExcelImportService(
                movementAddService,
                extractor,
                categoryAddService
        )

        // Stub default category
        categoryAddService.getDefaultCategory() >> "SIN_CATEGORIA"
    }

    def "match - should return true for SANTANDER"() {
        expect:
        service.match("SANTANDER")
        service.match("santander")
        service.match("Santander")
    }

    def "match - should return false for other banks"() {
        expect:
        !service.match("BBVA")
        !service.match("GALICIA")
        !service.match("OTHER")
    }

    def "getBank - should return SANTANDER"() {
        expect:
        service.getBank() == "SANTANDER"
    }

    def "process - should create DEBITO for negative amounts"() {
        given:
        byte[] fileContent = [0x00, 0x01] as byte[]
        Long workspaceId = 1L
        
        def eurCurrency = Currency.builder()
                .id(1L)
                .symbol("EUR")
                .description("Euro")
                .build()

        def expense = new ParsedExpense(
                LocalDate.of(2026, 4, 13),
                "Compra en tienda",
                null,
                null,
                eurCurrency,
                new BigDecimal("-123.45"),  // negativo = gasto
                BigDecimal.ZERO
        )

        extractor.parse(fileContent) >> [expense]

        when:
        service.process(fileContent, workspaceId)

        then:
        1 * movementAddService.saveExpenseAll(_ as List<MovementToAdd>) >> { List args ->
            def movements = args[0] as List<MovementToAdd>
            assert movements.size() == 1
            
            def movement = movements[0]
            assert movement.amount() == new BigDecimal("123.45")  // valor absoluto
            assert movement.type() == MovementType.DEBITO.name()
            assert movement.description() == "Compra en tienda"
            assert movement.category() == "SIN_CATEGORIA"
            assert movement.currency() == "EUR"
            assert movement.bank() == "SANTANDER"
            assert movement.cuotaActual() == 0
            assert movement.cuotasTotales() == 0
        }
    }

    def "process - should create INGRESO for positive amounts"() {
        given:
        byte[] fileContent = [0x00, 0x01] as byte[]
        Long workspaceId = 1L
        
        def eurCurrency = Currency.builder()
                .id(1L)
                .symbol("EUR")
                .description("Euro")
                .build()

        def expense = new ParsedExpense(
                LocalDate.of(2026, 4, 13),
                "Transferencia recibida",
                null,
                null,
                eurCurrency,
                new BigDecimal("1000.00"),  // positivo = ingreso
                BigDecimal.ZERO
        )

        extractor.parse(fileContent) >> [expense]

        when:
        service.process(fileContent, workspaceId)

        then:
        1 * movementAddService.saveExpenseAll(_ as List<MovementToAdd>) >> { List args ->
            def movements = args[0] as List<MovementToAdd>
            assert movements.size() == 1
            
            def movement = movements[0]
            assert movement.amount() == new BigDecimal("1000.00")
            assert movement.type() == MovementType.INGRESO.name()
            assert movement.description() == "Transferencia recibida"
            assert movement.category() == "SIN_CATEGORIA"
            assert movement.currency() == "EUR"
            assert movement.bank() == "SANTANDER"
        }
    }

    def "process - should handle multiple expenses"() {
        given:
        byte[] fileContent = [0x00, 0x01] as byte[]
        Long workspaceId = 1L
        
        def eurCurrency = Currency.builder()
                .id(1L)
                .symbol("EUR")
                .description("Euro")
                .build()

        def expense1 = new ParsedExpense(
                LocalDate.of(2026, 4, 13),
                "Gasto 1",
                null, null,
                eurCurrency,
                new BigDecimal("-100.00"),
                BigDecimal.ZERO
        )
        
        def expense2 = new ParsedExpense(
                LocalDate.of(2026, 4, 14),
                "Gasto 2",
                null, null,
                eurCurrency,
                new BigDecimal("-50.00"),
                BigDecimal.ZERO
        )

        extractor.parse(fileContent) >> [expense1, expense2]

        when:
        service.process(fileContent, workspaceId)

        then:
        1 * movementAddService.saveExpenseAll(_ as List<MovementToAdd>) >> { List args ->
            def movements = args[0] as List<MovementToAdd>
            assert movements.size() == 2
            assert movements[0].amount() == new BigDecimal("100.00")
            assert movements[1].amount() == new BigDecimal("50.00")
        }
    }

    def "process - should handle different currencies"() {
        given:
        byte[] fileContent = [0x00, 0x01] as byte[]
        Long workspaceId = 1L
        
        def usdCurrency = Currency.builder()
                .id(2L)
                .symbol("USD")
                .description("Dolar")
                .build()

        def expense = new ParsedExpense(
                LocalDate.of(2026, 4, 13),
                "Purchase in dollars",
                null, null,
                usdCurrency,
                new BigDecimal("-50.00"),
                BigDecimal.ZERO
        )

        extractor.parse(fileContent) >> [expense]

        when:
        service.process(fileContent, workspaceId)

        then:
        1 * movementAddService.saveExpenseAll(_ as List<MovementToAdd>) >> { List args ->
            def movements = args[0] as List<MovementToAdd>
            assert movements[0].currency() == "USD"
        }
    }
}
