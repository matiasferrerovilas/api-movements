package api.m2.movements.services.movements.files.strategies;

import api.m2.movements.enums.MovementType;
import api.m2.movements.helpers.ParserRegistry;
import api.m2.movements.records.pdf.ParsedExpense;
import api.m2.movements.services.category.CategoryAddService;
import api.m2.movements.services.movements.MovementAddService;
import api.m2.movements.services.movements.files.ExpenseFileStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Santander España: acepta tanto el export de movimientos de cuenta como el extracto de la
 * tarjeta de crédito. El parser informa el tipo de cada línea (DEBITO/INGRESO en cuenta,
 * CREDITO/REINTEGRO en tarjeta), así que {@link #getBankMethod()} solo queda como fallback.
 */
@Service
@Slf4j
public class SantanderImportService extends ExpenseFileStrategy {

    public SantanderImportService(MovementAddService movementAddService, ParserRegistry parserRegistry,
                                  CategoryAddService categoryAddService) {
        super(movementAddService, parserRegistry, categoryAddService);
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
    public MovementType getBankMethod() {
        return MovementType.DEBITO;
    }

    @Override
    protected BigDecimal resolveAmount(ParsedExpense e) {
        return e.amountPesos();
    }
}
