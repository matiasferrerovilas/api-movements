package api.m2.movements.services.movements.files;

import api.m2.movements.helpers.ExcelExtractorHelper;
import api.m2.movements.records.movements.MovementToAdd;
import api.m2.movements.records.pdf.ParsedExpense;
import api.m2.movements.services.category.CategoryAddService;
import api.m2.movements.services.movements.MovementAddService;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Estrategia abstracta para procesar archivos Excel/CSV bancarios.
 * Análoga a ExpenseFileStrategy pero para formatos estructurados (Excel/CSV).
 */
@RequiredArgsConstructor
public abstract class ExpenseExcelStrategy {
    protected final MovementAddService movementAddService;
    protected final ExcelExtractorHelper excelExtractor;
    protected final CategoryAddService categoryAddService;

    /**
     * Verifica si esta estrategia puede procesar el banco especificado.
     *
     * @param bank código del banco
     * @return true si esta estrategia maneja el banco
     */
    public abstract boolean match(String bank);

    /**
     * Retorna el código del banco asociado a esta estrategia.
     *
     * @return código del banco (ej: "SANTANDER")
     */
    public abstract String getBank();

    /**
     * Procesa el archivo Excel/CSV y guarda los movimientos.
     *
     * @param fileContent contenido binario del archivo
     * @param workspaceId ID del workspace donde se guardarán los movimientos
     */
    public void process(byte[] fileContent, Long workspaceId) {
        List<ParsedExpense> expenses = excelExtractor.parse(fileContent);

        List<MovementToAdd> movements = expenses.stream()
                .map(e -> this.processExpense(e, workspaceId))
                .toList();

        movementAddService.saveExpenseAll(movements);
    }

    /**
     * Procesa un gasto individual parseado y lo convierte a MovementToAdd.
     * Implementado por las estrategias concretas.
     *
     * @param expense gasto parseado del archivo
     * @param workspaceId ID del workspace
     * @return DTO para crear el movimiento
     */
    protected abstract MovementToAdd processExpense(ParsedExpense expense, Long workspaceId);
}
