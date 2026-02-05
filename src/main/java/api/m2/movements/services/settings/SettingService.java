package api.m2.movements.services.settings;

import api.m2.movements.enums.BanksEnum;
import api.m2.movements.enums.CategoryEnum;
import api.m2.movements.enums.MovementType;
import api.m2.movements.records.income.IncomeToAdd;
import api.m2.movements.records.movements.MovementToAdd;
import api.m2.movements.services.accounts.AccountQueryService;
import api.m2.movements.services.category.CategoryAddService;
import api.m2.movements.services.currencies.CurrencyAddService;
import api.m2.movements.services.movements.MovementAddService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
public class SettingService {
    private final MovementAddService movementAddService;
    private final CategoryAddService categoryAddService;
    private final CurrencyAddService currencyAddService;
    private final AccountQueryService accountQueryService;

    public void addIngreso(@Valid IncomeToAdd incomeToAdd) {
        var category = categoryAddService.findCategoryByDescription(CategoryEnum.HOGAR.getDescripcion());
        var account = accountQueryService.findAccountByName(incomeToAdd.group());
        String description = "Sueldo Recibido";
        var currency = currencyAddService.findBySymbol(incomeToAdd.currency());

        movementAddService.saveMovement(new MovementToAdd(incomeToAdd.amount(),
                LocalDate.now(),
                description,
                category.description(),
                MovementType.INGRESO.name(),
                currency.getSymbol(),
                0,
                0,
                BanksEnum.valueOf(incomeToAdd.bank()),
                account.getId()));
    }
}
