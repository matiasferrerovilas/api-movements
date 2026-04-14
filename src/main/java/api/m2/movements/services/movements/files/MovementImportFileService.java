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
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class MovementImportFileService {
    private final Set<ExpenseFileStrategy> expenseFileStrategies;
    private final PdfReaderService pdfReaderService;
    private final WorkspaceContextService workspaceContextService;

    public void importMovementsByFile(MultipartFile file, String bank) {
        Path pdfFile = null;
        try {
            pdfFile = Files.createTempFile("expense-", ".pdf");
            file.transferTo(pdfFile);
            String text = pdfReaderService.extractTextFromPdf(pdfFile);

            var list = expenseFileStrategies.stream()
                    .filter(strategy -> strategy.match(bank))
                    .toList();

            var workspaceId = workspaceContextService.getActiveWorkspaceId();
            var movementFile = new MovementFileToAdd(text, workspaceId);
            switch (list.size()) {
                case 0 -> throw new IllegalArgumentException("Invalid bank method");
                case 1 -> list.getFirst().process(movementFile);
                default -> throw new IllegalArgumentException("Multiple strategies found for bank method");
            }
        } catch (IOException _) {
            throw new BusinessException("No se pudo procesar");
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
}