package api.m2.movements.services.settings;

import api.m2.movements.entities.Account;
import api.m2.movements.entities.Currency;
import api.m2.movements.enums.CategoryEnum;
import api.m2.movements.enums.MovementType;
import api.m2.movements.records.categories.CategoryRecord;
import api.m2.movements.records.income.IncomeToAdd;
import api.m2.movements.records.movements.MovementToAdd;
import api.m2.movements.services.groups.AccountQueryService;
import api.m2.movements.services.category.CategoryAddService;
import api.m2.movements.services.currencies.CurrencyAddService;
import api.m2.movements.services.movements.MovementAddService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@Slf4j
@RequiredArgsConstructor
public class SettingService {
    private final MovementAddService movementAddService;
    private final CategoryAddService categoryAddService;
    private final CurrencyAddService currencyAddService;
    private final AccountQueryService accountQueryService;

    public void addIngreso(@Valid IncomeToAdd incomeToAdd) {
        CategoryRecord category = categoryAddService.findCategoryByDescription(CategoryEnum.HOGAR.getDescripcion());
        Account account = accountQueryService.findAccountByName(incomeToAdd.group());
        String description = "Sueldo Recibido";
        Currency currency = currencyAddService.findBySymbol(incomeToAdd.currency());

        movementAddService.saveMovement(new MovementToAdd(incomeToAdd.amount(),
                LocalDate.now(ZoneOffset.UTC),
                description,
                category.description(),
                MovementType.INGRESO.name(),
                currency.getSymbol(),
                0,
                0,
                incomeToAdd.bank(),
                account.getId()));
    }
}
