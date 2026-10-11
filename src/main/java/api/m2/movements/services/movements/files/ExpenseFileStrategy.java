package api.m2.movements.services.movements.files;

import api.m2.movements.helpers.ParserRegistry;
import api.m2.movements.records.categories.CategoryUpdateRecord;
import api.m2.movements.records.movements.ImportResultRecord;
import api.m2.movements.records.movements.MovementFileToAdd;
import api.m2.movements.records.movements.MovementToAdd;
import api.m2.movements.records.pdf.ParsedExpense;
import api.m2.movements.services.category.CategoryAddService;
import api.m2.movements.services.category.rules.CategoryMatcher;
import api.m2.movements.services.category.rules.CategoryRuleService;
import api.m2.movements.services.movements.MovementAddService;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public abstract class ExpenseFileStrategy {
    // Los movimientos importados no llevan plan de cuotas: el extracto ya trae cada cuota como su
    // propia línea, y si se informaran CreditInstallmentJob generaría la siguiente por duplicado.
    private static final int NO_INSTALLMENTS = 0;
    // Largo de movements.description en la base. Los conceptos de transferencias y recibos pueden
    // superarlo y una sola fila larga haría fallar todo el import (saveExpenseAll es transaccional).
    private static final int MAX_DESCRIPTION_LENGTH = 60;

    protected final MovementAddService movementAddService;
    protected final ParserRegistry parserRegistry;
    protected final CategoryAddService categoryAddService;
    protected final CategoryRuleService categoryRuleService;

    public abstract boolean match(String bank);

    public abstract String getBank();

    public ImportResultRecord process(MovementFileToAdd movementFileToAdd) {
        var parser = parserRegistry.getParser(this.getBank());

        List<ParsedExpense> expenses = parser.parse(movementFileToAdd.file());
        var matcher = categoryRuleService.matcherFor(movementFileToAdd.workspaceId());

        return movementAddService.saveExpenseAll(expenses.stream().map(e -> this.processExpense(e, matcher)).toList());
    }

    private MovementToAdd processExpense(ParsedExpense e, CategoryMatcher matcher) {
        // Primero lo aprendido de las correcciones del usuario; si no hay regla, el default fijo.
        var category = matcher.match(e.reference())
                .orElseGet(() -> categoryAddService.resolveDefaultCategory(e.reference()).description());
        return new MovementToAdd(
                e.amount(),
                e.date(),
                this.truncateDescription(e.reference()),
                List.of(new CategoryUpdateRecord(null, category)),
                e.type().name(),
                e.currency().getSymbol(),
                NO_INSTALLMENTS,
                NO_INSTALLMENTS,
                this.getBank(),
                null,
                null
        );
    }

    private String truncateDescription(String description) {
        if (description.length() <= MAX_DESCRIPTION_LENGTH) {
            return description;
        }
        return description.substring(0, MAX_DESCRIPTION_LENGTH).trim();
    }
}
