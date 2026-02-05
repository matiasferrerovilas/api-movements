package api.m2.movements.services.movements.files.strategies;

import api.m2.movements.enums.BanksEnum;
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
