package api.expenses.expenses.services.movements;

import api.expenses.expenses.enums.BanksEnum;
import api.expenses.expenses.exceptions.BusinessException;
import api.expenses.expenses.helpers.PdfReaderService;
import api.expenses.expenses.records.movements.MovementFileToAdd;
import api.expenses.expenses.records.movements.MovementRecord;
import api.expenses.expenses.services.movements.files.ExpenseFileStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class MovementImportFileService {
    private final Set<ExpenseFileStrategy> expenseFileStrategies;

    private final PdfReaderService pdfReaderService;
    public List<MovementRecord> importMovementsByFile(MultipartFile file, String bank, String group) {
        try {
            File pdfFile = File.createTempFile("expense-", ".pdf");
            file.transferTo(pdfFile);
            String text = pdfReaderService.extractTextFromPdf(pdfFile);
            pdfFile.delete();

            BanksEnum banksEnum = BanksEnum.valueOf(bank.toUpperCase());
            var list = expenseFileStrategies.stream()
                    .filter(strategy -> strategy.match(banksEnum))
                    .toList();

            var movementFile = new MovementFileToAdd(text, group);
            return switch (list.size()) {
                case 0 -> throw new IllegalArgumentException("Invalid bank method");
                case 1 -> list.getFirst().process(movementFile);
                default -> throw new IllegalArgumentException("Multiple strategies found for bank method");
            };
        } catch (IOException e) {
            throw new BusinessException("No se pudo procesar");
        }
    }
}