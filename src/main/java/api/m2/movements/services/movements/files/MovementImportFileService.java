package api.m2.movements.services.movements.files;

import api.m2.movements.exceptions.BusinessException;
import api.m2.movements.helpers.PdfReaderService;
import api.m2.movements.records.movements.MovementFileToAdd;
import api.m2.movements.services.workspaces.WorkspaceQueryService;
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
    private final WorkspaceQueryService workspaceQueryService;

    /*
    todo: Terminar es tarea 21, ver ventajas desventajas de implementar rabbit con IA
    public void sendToRabbit(MultipartFile file, String bank, String group) {
        try {
            Path pdfFile = Files.createTempFile("expense-", ".pdf");
            file.transferTo(pdfFile);
            String text = pdfReaderService.extractTextFromPdf(pdfFile);
            Files.deleteIfExists(pdfFile);

            BanksEnum banksEnum = BanksEnum.valueOf(bank.toUpperCase());

            var user = userService.getAuthenticatedUserRecord();
            movementPublishServiceRabbit.publishMovementFile(new MovementFileRequest(text,
                    banksEnum,
                    group,
                    user.id(),
                    LocalDateTime.now()));

        } catch (IOException e) {
            throw new BusinessException("No se pudo procesar");
        }
    }
    public void processList(List<CreditCardStatement> creditCardStatements) {
        creditCardStatements.forEach(movement -> {
        });
    }*/
    public void importMovementsByFile(MultipartFile file, String bank, Long workspaceId) {
        Path pdfFile = null;
        try {
            pdfFile = Files.createTempFile("expense-", ".pdf");
            file.transferTo(pdfFile);
            String text = pdfReaderService.extractTextFromPdf(pdfFile);

            var list = expenseFileStrategies.stream()
                    .filter(strategy -> strategy.match(bank))
                    .toList();

            var workspace = workspaceQueryService.findWorkspaceById(workspaceId);
            var movementFile = new MovementFileToAdd(text, workspace.getId());
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