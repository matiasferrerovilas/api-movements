package api.m2.movements.services.movements.files.strategies;

import api.m2.movements.helpers.ParserRegistry;
import api.m2.movements.services.category.CategoryAddService;
import api.m2.movements.services.category.rules.CategoryRuleService;
import api.m2.movements.services.movements.MovementAddService;
import api.m2.movements.services.movements.files.ExpenseFileStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Santander España: acepta tanto el export de movimientos de cuenta como el extracto de la
 * tarjeta de crédito; {@code SantanderPdfExtractorHelper} detecta cuál es.
 */
@Service
@Slf4j
public class SantanderImportService extends ExpenseFileStrategy {

    public SantanderImportService(MovementAddService movementAddService, ParserRegistry parserRegistry,
                                  CategoryAddService categoryAddService, CategoryRuleService categoryRuleService) {
        super(movementAddService, parserRegistry, categoryAddService, categoryRuleService);
    }

    @Override
    public boolean match(String bank) {
        return "SANTANDER".equalsIgnoreCase(bank);
    }

    @Override
    public String getBank() {
        return "SANTANDER";
    }
}
