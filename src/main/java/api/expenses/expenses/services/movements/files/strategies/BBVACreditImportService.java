package api.expenses.expenses.services.movements.files.strategies;

import api.expenses.expenses.enums.BanksEnum;
import api.expenses.expenses.enums.CurrencyEnum;
import api.expenses.expenses.enums.MovementType;
import api.expenses.expenses.helpers.ParserRegistry;
import api.expenses.expenses.records.pdf.ParsedExpense;
import api.expenses.expenses.services.category.CategoryAddService;
import api.expenses.expenses.services.movements.MovementAddService;
import api.expenses.expenses.services.movements.files.ExpenseFileStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class BBVACreditImportService extends ExpenseFileStrategy {


    public BBVACreditImportService(MovementAddService movementAddService, ParserRegistry parserRegistry, CategoryAddService categoryAddService) {
        super(movementAddService, parserRegistry, categoryAddService);
    }

    @Override
    public boolean match(BanksEnum banksEnum) {
        return BanksEnum.BBVA.equals(banksEnum);
    }
    @Override
    public MovementType getBankMethod() {
        return MovementType.CREDITO;
    }
    @Override
    public BanksEnum getBank() {
        return BanksEnum.BBVA;
    }

    @Override
    protected BigDecimal resolveAmount(ParsedExpense e) {
        return CurrencyEnum.USD.equals(CurrencyEnum.valueOf(e.currency().getSymbol()))
                ? e.amountDolares()
                : e.amountPesos();
    }
}
