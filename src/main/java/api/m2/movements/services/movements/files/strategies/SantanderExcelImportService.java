package api.m2.movements.services.movements.files.strategies;

import api.m2.movements.enums.MovementType;
import api.m2.movements.helpers.SantanderExcelExtractorHelper;
import api.m2.movements.records.movements.MovementToAdd;
import api.m2.movements.records.pdf.ParsedExpense;
import api.m2.movements.services.category.CategoryAddService;
import api.m2.movements.services.movements.MovementAddService;
import api.m2.movements.services.movements.files.ExpenseExcelStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Estrategia de importación para archivos Excel/CSV del banco Santander.
 *
 * Características:
 * - Tipo de movimiento inferido del signo del importe (negativo → DEBITO, positivo → INGRESO)
 * - Categoría por defecto: SIN_CATEGORIA
 * - Moneda: extraída del archivo
 * - Soporta formatos: XLS, XLSX, CSV
 */
@Service
@Slf4j
public class SantanderExcelImportService extends ExpenseExcelStrategy {

    public SantanderExcelImportService(
            MovementAddService movementAddService,
            SantanderExcelExtractorHelper excelExtractor,
            CategoryAddService categoryAddService) {
        super(movementAddService, excelExtractor, categoryAddService);
    }

    @Override
    public boolean match(String bank) {
        return "SANTANDER".equalsIgnoreCase(bank);
    }

    @Override
    public String getBank() {
        return "SANTANDER";
    }

    @Override
    protected MovementToAdd processExpense(ParsedExpense expense, Long workspaceId) {
        // Determinar tipo de movimiento según signo del importe
        // Negativo → DEBITO (gasto)
        // Positivo → INGRESO
        BigDecimal amount = expense.amountPesos();
        MovementType type = amount.compareTo(BigDecimal.ZERO) < 0
                ? MovementType.DEBITO
                : MovementType.INGRESO;

        // Categoría por defecto: SIN_CATEGORIA
        String categoryDescription = categoryAddService.getDefaultCategory();

        log.debug("Processing Santander expense: date={}, amount={}, type={}, currency={}",
                expense.date(), amount, type, expense.currency().getSymbol());

        return new MovementToAdd(
                amount.abs(),                   // valor absoluto del importe
                expense.date(),                 // fecha de la operación
                expense.reference(),            // concepto/descripción
                categoryDescription,            // SIN_CATEGORIA
                type.name(),                    // DEBITO o INGRESO
                expense.currency().getSymbol(), // EUR, USD, etc.
                0,                              // cuotaActual (no aplica)
                0,                              // cuotaTotales (no aplica)
                this.getBank()                  // SANTANDER
        );
    }
}
