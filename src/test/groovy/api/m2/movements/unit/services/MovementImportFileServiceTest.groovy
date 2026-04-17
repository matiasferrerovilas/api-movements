package api.m2.movements.unit.services

import api.m2.movements.exceptions.BusinessException
import api.m2.movements.helpers.PdfReaderService
import api.m2.movements.records.movements.MovementFileToAdd
import api.m2.movements.services.movements.files.ExpenseExcelStrategy
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
    ExpenseExcelStrategy santanderStrategy = Mock(ExpenseExcelStrategy)
    Set<ExpenseFileStrategy> pdfStrategies
    Set<ExpenseExcelStrategy> excelStrategies

    MovementImportFileService service

    def setup() {
        pdfStrategies = [bbvaStrategy, galiciaStrategy] as Set
        excelStrategies = [santanderStrategy] as Set
        service = new MovementImportFileService(
                pdfStrategies,
                excelStrategies,
                pdfReaderService,
                workspaceContextService
        )
    }

    def "importMovementsByFile - should process file with matching strategy"() {
        given:
        def file = Mock(MultipartFile)
        def bank = "BBVA"
        def pdfText = "PDF content"
        def workspaceId = 1L

        file.getOriginalFilename() >> "movements.pdf"
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

        file.getOriginalFilename() >> "movements.pdf"
        file.transferTo(_ as java.nio.file.Path) >> {}
        pdfReaderService.extractTextFromPdf(_ as java.nio.file.Path) >> pdfText
        workspaceContextService.getActiveWorkspaceId() >> 1L
        bbvaStrategy.match(bank) >> false
        galiciaStrategy.match(bank) >> false

        when:
        service.importMovementsByFile(file, bank)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Banco no soportado para PDF: UNKNOWN_BANK"
    }

    def "importMovementsByFile - should throw IllegalArgumentException when multiple strategies match"() {
        given:
        def file = Mock(MultipartFile)
        def bank = "AMBIGUOUS_BANK"
        def pdfText = "PDF content"

        file.getOriginalFilename() >> "movements.pdf"
        file.transferTo(_ as java.nio.file.Path) >> {}
        pdfReaderService.extractTextFromPdf(_ as java.nio.file.Path) >> pdfText
        workspaceContextService.getActiveWorkspaceId() >> 1L
        bbvaStrategy.match(bank) >> true
        galiciaStrategy.match(bank) >> true

        when:
        service.importMovementsByFile(file, bank)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Múltiples estrategias encontradas para: AMBIGUOUS_BANK"
    }

    def "importMovementsByFile - should throw BusinessException when IOException occurs during file transfer"() {
        given:
        def file = Mock(MultipartFile)
        def bank = "BBVA"

        file.getOriginalFilename() >> "movements.pdf"
        file.transferTo(_ as java.nio.file.Path) >> { throw new IOException("Transfer failed") }

        when:
        service.importMovementsByFile(file, bank)

        then:
        def ex = thrown(BusinessException)
        ex.message == "Error al procesar el archivo: Transfer failed"
    }

    def "importMovementsByFile - should throw BusinessException when IOException occurs during PDF reading"() {
        given:
        def file = Mock(MultipartFile)
        def bank = "BBVA"

        file.getOriginalFilename() >> "movements.pdf"
        file.transferTo(_ as java.nio.file.Path) >> {}
        pdfReaderService.extractTextFromPdf(_ as java.nio.file.Path) >> { throw new IOException("Read failed") }

        when:
        service.importMovementsByFile(file, bank)

        then:
        def ex = thrown(BusinessException)
        ex.message == "Error al procesar el archivo: Read failed"
    }

    def "importMovementsByFile - should use GALICIA strategy when bank is GALICIA"() {
        given:
        def file = Mock(MultipartFile)
        def bank = "GALICIA"
        def pdfText = "Galicia PDF content"
        def workspaceId = 2L

        file.getOriginalFilename() >> "movements.pdf"
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

    def "importMovementsByFile - should process Excel file with SANTANDER"() {
        given:
        def file = Mock(MultipartFile)
        def bank = "SANTANDER"
        def workspaceId = 3L
        byte[] fileContent = [0x00, 0x01, 0x02] as byte[]

        file.getOriginalFilename() >> "movements.xlsx"
        file.getBytes() >> fileContent
        workspaceContextService.getActiveWorkspaceId() >> workspaceId
        santanderStrategy.match(bank) >> true

        when:
        service.importMovementsByFile(file, bank)

        then:
        1 * santanderStrategy.process(fileContent, workspaceId)
    }

    def "importMovementsByFile - should throw BusinessException for unsupported file extension"() {
        given:
        def file = Mock(MultipartFile)
        def bank = "BBVA"

        file.getOriginalFilename() >> "movements.txt"

        when:
        service.importMovementsByFile(file, bank)

        then:
        def ex = thrown(BusinessException)
        ex.message.contains("Formato de archivo no soportado")
    }
}
