package api.m2.movements.unit.services

import api.m2.movements.exceptions.BusinessException
import api.m2.movements.helpers.PdfReaderService
import api.m2.movements.records.movements.MovementFileToAdd
import api.m2.movements.services.movements.files.ExpenseFileStrategy
import api.m2.movements.services.movements.files.MovementImportFileService
import api.m2.movements.services.workspaces.WorkspaceContextService
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification

class MovementImportFileServiceTest extends Specification {

    PdfReaderService pdfReaderService = Mock(PdfReaderService)
    WorkspaceContextService workspaceContextService = Mock(WorkspaceContextService)
    ExpenseFileStrategy bbvaStrategy = Mock(ExpenseFileStrategy)
    ExpenseFileStrategy galiciaStrategy = Mock(ExpenseFileStrategy)
    Set<ExpenseFileStrategy> strategies

    MovementImportFileService service

    def setup() {
        strategies = [bbvaStrategy, galiciaStrategy] as Set
        service = new MovementImportFileService(strategies, pdfReaderService, workspaceContextService)
    }

    def "importMovementsByFile - should process file with matching strategy"() {
        given:
        def file = Mock(MultipartFile)
        def bank = "BBVA"
        def pdfText = "PDF content"
        def workspaceId = 1L

        file.transferTo(_ as java.nio.file.Path) >> {}
        pdfReaderService.extractTextFromPdf(_ as java.nio.file.Path) >> pdfText
        workspaceContextService.getActiveWorkspaceId() >> workspaceId
        bbvaStrategy.match(bank) >> true
        galiciaStrategy.match(bank) >> false

        when:
        service.importMovementsByFile(file, bank)

        then:
        1 * bbvaStrategy.process(_ as MovementFileToAdd) >> { MovementFileToAdd movementFile ->
            assert movementFile.file() == pdfText
            assert movementFile.workspaceId() == workspaceId
        }
        0 * galiciaStrategy.process(_)
    }

    def "importMovementsByFile - should throw IllegalArgumentException when no strategy matches"() {
        given:
        def file = Mock(MultipartFile)
        def bank = "UNKNOWN_BANK"
        def pdfText = "PDF content"

        file.transferTo(_ as java.nio.file.Path) >> {}
        pdfReaderService.extractTextFromPdf(_ as java.nio.file.Path) >> pdfText
        workspaceContextService.getActiveWorkspaceId() >> 1L
        bbvaStrategy.match(bank) >> false
        galiciaStrategy.match(bank) >> false

        when:
        service.importMovementsByFile(file, bank)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Invalid bank method"
    }

    def "importMovementsByFile - should throw IllegalArgumentException when multiple strategies match"() {
        given:
        def file = Mock(MultipartFile)
        def bank = "AMBIGUOUS_BANK"
        def pdfText = "PDF content"

        file.transferTo(_ as java.nio.file.Path) >> {}
        pdfReaderService.extractTextFromPdf(_ as java.nio.file.Path) >> pdfText
        workspaceContextService.getActiveWorkspaceId() >> 1L
        bbvaStrategy.match(bank) >> true
        galiciaStrategy.match(bank) >> true

        when:
        service.importMovementsByFile(file, bank)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Multiple strategies found for bank method"
    }

    def "importMovementsByFile - should throw BusinessException when IOException occurs during file transfer"() {
        given:
        def file = Mock(MultipartFile)
        def bank = "BBVA"

        file.transferTo(_ as java.nio.file.Path) >> { throw new IOException("Transfer failed") }

        when:
        service.importMovementsByFile(file, bank)

        then:
        def ex = thrown(BusinessException)
        ex.message == "No se pudo procesar"
    }

    def "importMovementsByFile - should throw BusinessException when IOException occurs during PDF reading"() {
        given:
        def file = Mock(MultipartFile)
        def bank = "BBVA"

        file.transferTo(_ as java.nio.file.Path) >> {}
        pdfReaderService.extractTextFromPdf(_ as java.nio.file.Path) >> { throw new IOException("Read failed") }

        when:
        service.importMovementsByFile(file, bank)

        then:
        def ex = thrown(BusinessException)
        ex.message == "No se pudo procesar"
    }

    def "importMovementsByFile - should use GALICIA strategy when bank is GALICIA"() {
        given:
        def file = Mock(MultipartFile)
        def bank = "GALICIA"
        def pdfText = "Galicia PDF content"
        def workspaceId = 2L

        file.transferTo(_ as java.nio.file.Path) >> {}
        pdfReaderService.extractTextFromPdf(_ as java.nio.file.Path) >> pdfText
        workspaceContextService.getActiveWorkspaceId() >> workspaceId
        bbvaStrategy.match(bank) >> false
        galiciaStrategy.match(bank) >> true

        when:
        service.importMovementsByFile(file, bank)

        then:
        0 * bbvaStrategy.process(_)
        1 * galiciaStrategy.process(_ as MovementFileToAdd) >> { MovementFileToAdd movementFile ->
            assert movementFile.file() == pdfText
            assert movementFile.workspaceId() == workspaceId
        }
    }
}
