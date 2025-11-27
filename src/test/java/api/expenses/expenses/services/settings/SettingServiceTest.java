package api.expenses.expenses.services.settings;

import api.expenses.expenses.entities.Currency;
import api.expenses.expenses.entities.UserGroups;
import api.expenses.expenses.enums.BanksEnum;
import api.expenses.expenses.enums.CategoryEnum;
import api.expenses.expenses.enums.CurrencyEnum;
import api.expenses.expenses.enums.GroupsEnum;
import api.expenses.expenses.enums.MovementType;
import api.expenses.expenses.records.IngresoToAdd;
import api.expenses.expenses.records.categories.CategoryRecord;
import api.expenses.expenses.records.movements.MovementToAdd;
import api.expenses.expenses.services.category.CategoryAddService;
import api.expenses.expenses.services.currencies.CurrencyAddService;
import api.expenses.expenses.services.groups.GroupGetService;
import api.expenses.expenses.services.movements.MovementAddService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingServiceTest {

    @Mock
    private MovementAddService movementAddService;
    @Mock
    private CurrencyAddService currencyAddService;
    @Mock
    private CategoryAddService categoryAddService;
    @Mock
    private GroupGetService groupGetService;

    @InjectMocks
    private SettingService settingService;

    @Test
    @DisplayName("Agrego Ingreso correctamente")
    void addIngreso() {
        var ingresoToAdd = new IngresoToAdd(BanksEnum.GALICIA.name(),
                CurrencyEnum.ARS.name(),
                new BigDecimal("150000.0"),
                GroupsEnum.DEFAULT.name());

        when(categoryAddService.findCategoryByDescription(CategoryEnum.HOGAR.getDescripcion()))
                .thenReturn(new CategoryRecord("Hogar"));
        when(currencyAddService.findBySymbol(ingresoToAdd.currency()))
                .thenReturn(Currency.builder().symbol("ARS").build());
        when(groupGetService.getGroupByDescription(ingresoToAdd.group()))
                .thenReturn(UserGroups.builder().description("DEFAULT").build());

        var movementToAdd = new MovementToAdd(ingresoToAdd.amount(),
                LocalDate.now(),
                "Sueldo Recibido",
                "Hogar",
                MovementType.INGRESO.name(),
                "ARS",
                0,
                0,
                BanksEnum.valueOf(ingresoToAdd.bank()),
                "DEFAULT");

        settingService.addIngreso(ingresoToAdd);
        verify(movementAddService).saveMovement(movementToAdd);
    }
}