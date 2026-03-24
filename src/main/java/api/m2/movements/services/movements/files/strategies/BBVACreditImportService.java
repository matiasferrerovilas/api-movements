package api.m2.movements.services.movements.files.strategies;

import api.m2.movements.enums.CurrencyEnum;
import api.m2.movements.enums.MovementType;
import api.m2.movements.helpers.ParserRegistry;
import api.m2.movements.records.pdf.ParsedExpense;
import api.m2.movements.services.category.CategoryAddService;
import api.m2.movements.services.movements.MovementAddService;
import api.m2.movements.services.movements.files.ExpenseFileStrategy;
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
    public boolean match(String bank) {
        return "BBVA".equalsIgnoreCase(bank);
    }
    @Override
    public MovementType getBankMethod() {
        return MovementType.CREDITO;
    }
    @Override
    public String getBank() {
        return "BBVA";
    }

    @Override
    protected BigDecimal resolveAmount(ParsedExpense e) {
        return CurrencyEnum.USD.equals(CurrencyEnum.valueOf(e.currency().getSymbol()))
                ? e.amountDolares()
                : e.amountPesos();
    }
}
