package api.m2.movements.services.movements.files;

import api.m2.movements.enums.BanksEnum;
import api.m2.movements.enums.MovementType;
import api.m2.movements.helpers.ParserRegistry;
import api.m2.movements.records.movements.MovementFileToAdd;
import api.m2.movements.records.movements.MovementToAdd;
import api.m2.movements.records.pdf.ParsedExpense;
import api.m2.movements.services.category.CategoryAddService;
import api.m2.movements.services.movements.MovementAddService;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@RequiredArgsConstructor
public abstract class ExpenseFileStrategy {
    protected final MovementAddService movementAddService;
    protected final ParserRegistry parserRegistry;
    protected final CategoryAddService categoryAddService;

    public abstract boolean match(BanksEnum banksEnum);

    public abstract BanksEnum getBank();

    public abstract MovementType getBankMethod();

    protected abstract BigDecimal resolveAmount(ParsedExpense e);

    public void process(MovementFileToAdd movementFileToAdd) {
        var parser = parserRegistry.getParser(this.getBank());

        List<ParsedExpense> expenses = parser.parse(movementFileToAdd.file());

        movementAddService.saveExpenseAll(expenses.stream().map(m -> this.processExpense(m, movementFileToAdd))
                .toList());
    }

    private MovementToAdd processExpense(ParsedExpense e, MovementFileToAdd movementFileToAdd) {
        int cuotaActual = 0;
        int cuotaTotales = 0;

        if (e.installment() != null && !e.installment().isEmpty()) {
            String[] parts = e.installment().split("/");
            cuotaActual = Integer.parseInt(parts[0]);
            cuotaTotales = Integer.parseInt(parts[1]);
        }

        var categoryDefault = categoryAddService.getCategoryAtLoadDefaultByStringHelper(e.reference());
        return new MovementToAdd(
                this.resolveAmount(e),
                e.date(),
                e.reference(),
                categoryDefault.description(),
                this.getBankMethod().name(),
                e.currency().getSymbol(),
                cuotaActual,
                cuotaTotales,
                getBank(),
                movementFileToAdd.accountId()
        );

    }
}
