package api.m2.movements.services.movements.files.strategies;

import api.m2.movements.enums.BanksEnum;
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
public class GaliciaCreditImportService extends ExpenseFileStrategy {


    public GaliciaCreditImportService(MovementAddService movementAddService, ParserRegistry parserRegistry, CategoryAddService categoryAddService) {
        super(movementAddService, parserRegistry, categoryAddService);
    }

    @Override
    public boolean match(BanksEnum banksEnum) {
        return BanksEnum.GALICIA.equals(banksEnum);
    }

    @Override
    public BanksEnum getBank() {
        return BanksEnum.GALICIA;
    }

    @Override
    public MovementType getBankMethod() {
        return MovementType.CREDITO;
    }

    @Override
    protected BigDecimal resolveAmount(ParsedExpense e) {
        return e.amountPesos() == null ? e.amountDolares() : e.amountPesos();
    }
}