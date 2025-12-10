package api.expenses.expenses.services.settings;

import api.expenses.expenses.enums.BanksEnum;
import api.expenses.expenses.enums.CategoryEnum;
import api.expenses.expenses.enums.MovementType;
import api.expenses.expenses.records.income.IncomeToAdd;
import api.expenses.expenses.records.movements.MovementToAdd;
import api.expenses.expenses.services.category.CategoryAddService;
import api.expenses.expenses.services.currencies.CurrencyAddService;
import api.expenses.expenses.services.groups.GroupGetService;
import api.expenses.expenses.services.movements.MovementAddService;
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
    private final GroupGetService groupGetService;

    public void addIngreso(@Valid IncomeToAdd incomeToAdd) {
        var category = categoryAddService.findCategoryByDescription(CategoryEnum.HOGAR.getDescripcion());
        var descriptionGroup = groupGetService.getGroupByDescription(incomeToAdd.group());
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
                descriptionGroup.getDescription()));
    }
}
