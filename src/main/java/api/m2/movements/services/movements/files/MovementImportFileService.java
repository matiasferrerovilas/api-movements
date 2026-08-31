package api.m2.movements.services.movements.files;

import api.m2.movements.exceptions.BusinessException;
import api.m2.movements.exceptions.RateLimitExceededException;
import api.m2.movements.helpers.PdfReaderService;
import api.m2.movements.records.movements.MovementFileToAdd;
import api.m2.movements.services.ratelimit.RateLimiterService;
import api.m2.movements.services.workspaces.WorkspaceContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class MovementImportFileService {
    // Más estricto que el límite genérico de RateLimitInterceptor a propósito: esto no es un CRUD
    // liviano, es escribir el archivo a disco y parsearlo entero con PDFBox — un usuario real
    // importa un puñado de extractos por sesión, no docenas por minuto.
    private static final int MAX_IMPORTS_PER_HOUR = 10;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofHours(1);

    private final Set<ExpenseFileStrategy> expenseFileStrategies;
    private final PdfReaderService pdfReaderService;
    private final WorkspaceContextService workspaceContextService;
    private final RateLimiterService rateLimiterService;

    public void importMovementsByFile(MultipartFile file, String bank) {
        enforceImportRateLimit();

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
                case 0 -> throw new BusinessException("Invalid bank method");
                case 1 -> list.getFirst().process(movementFile);
                default -> throw new BusinessException("Multiple strategies found for bank method");
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

    private void enforceImportRateLimit() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        String key = "rate-limit:movement-import:" + email;
        if (!rateLimiterService.tryAcquire(key, MAX_IMPORTS_PER_HOUR, RATE_LIMIT_WINDOW)) {
            throw new RateLimitExceededException("Alcanzaste el límite de importaciones de extracto. Probá de nuevo más tarde.");
        }
    }
}