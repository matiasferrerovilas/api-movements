package api.expenses.expenses.services.movements.files;

import api.expenses.expenses.enums.BanksEnum;
import api.expenses.expenses.exceptions.BusinessException;
import api.expenses.expenses.helpers.PdfReaderService;
import api.expenses.expenses.records.movements.MovementFileToAdd;
import api.expenses.expenses.services.accounts.AccountQueryService;
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
    private final AccountQueryService accountQueryService;

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
    public void importMovementsByFile(MultipartFile file, String bank, Long accountId) {
        try {
            Path pdfFile = Files.createTempFile("expense-", ".pdf");
            file.transferTo(pdfFile);
            String text = pdfReaderService.extractTextFromPdf(pdfFile);
            Files.deleteIfExists(pdfFile);

            BanksEnum banksEnum = BanksEnum.valueOf(bank.toUpperCase());
            var list = expenseFileStrategies.stream()
                    .filter(strategy -> strategy.match(banksEnum))
                    .toList();

            var account = accountQueryService.findAccountById(accountId);
            var movementFile = new MovementFileToAdd(text, account.getId());
            switch (list.size()) {
                case 0 -> throw new IllegalArgumentException("Invalid bank method");
                case 1 -> list.getFirst().process(movementFile);
                default -> throw new IllegalArgumentException("Multiple strategies found for bank method");
            }
        } catch (IOException e) {
            throw new BusinessException("No se pudo procesar");
        }
    }
}