package api.m2.movements.services.movements.files;

import api.m2.movements.exceptions.BusinessException;
import api.m2.movements.helpers.PdfReaderService;
import api.m2.movements.records.movements.MovementFileToAdd;
import api.m2.movements.services.workspaces.WorkspaceContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class MovementImportFileService {
    private final Set<ExpenseFileStrategy> pdfStrategies;
    private final Set<ExpenseExcelStrategy> excelStrategies;
    private final PdfReaderService pdfReaderService;
    private final WorkspaceContextService workspaceContextService;

    public void importMovementsByFile(MultipartFile file, String bank) {
        String extension = this.getFileExtension(file.getOriginalFilename());
        Long workspaceId = workspaceContextService.getActiveWorkspaceId();

        try {
            if ("pdf".equalsIgnoreCase(extension)) {
                this.importPdf(file, bank, workspaceId);
            } else if (List.of("xls", "xlsx", "csv").contains(extension.toLowerCase())) {
                this.importExcel(file, bank, workspaceId);
            } else {
                throw new BusinessException("Formato de archivo no soportado: " + extension
                        + ". Formatos válidos: PDF, XLS, XLSX, CSV");
            }
        } catch (IOException e) {
            log.error("Error procesando archivo: {}", file.getOriginalFilename(), e);
            throw new BusinessException("Error al procesar el archivo: " + e.getMessage());
        }
    }

    private void importPdf(MultipartFile file, String bank, Long workspaceId) throws IOException {
        Path pdfFile = null;
        try {
            pdfFile = Files.createTempFile("expense-", ".pdf");
            file.transferTo(pdfFile);
            String text = pdfReaderService.extractTextFromPdf(pdfFile);

            var strategies = pdfStrategies.stream()
                    .filter(strategy -> strategy.match(bank))
                    .toList();

            var movementFile = new MovementFileToAdd(text, workspaceId);
            switch (strategies.size()) {
                case 0 -> throw new IllegalArgumentException("Banco no soportado para PDF: " + bank);
                case 1 -> strategies.getFirst().process(movementFile);
                default -> throw new IllegalArgumentException("Múltiples estrategias encontradas para: " + bank);
            }
        } finally {
            if (pdfFile != null) {
                try {
                    Files.deleteIfExists(pdfFile);
                } catch (IOException e) {
                    log.warn("No se pudo eliminar el archivo temporal: {}", pdfFile, e);
                }
            }
        }
    }

    private void importExcel(MultipartFile file, String bank, Long workspaceId) throws IOException {
        byte[] fileContent = file.getBytes();

        ExpenseExcelStrategy strategy = excelStrategies.stream()
                .filter(s -> s.match(bank))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Banco no soportado para Excel/CSV: " + bank));

        log.info("Processing Excel/CSV file: {} for bank: {}", file.getOriginalFilename(), bank);
        strategy.process(fileContent, workspaceId);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new BusinessException("Nombre de archivo inválido: " + filename);
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}