package api.expenses.expenses.services.balance;

import api.expenses.expenses.enums.BalanceEnum;
import api.expenses.expenses.enums.MovementType;
import api.expenses.expenses.records.BalanceByCategoryRecord;
import api.expenses.expenses.records.balance.BalanceFilterRecord;
import api.expenses.expenses.repositories.CurrencyRepository;
import api.expenses.expenses.repositories.MovementRepository;
import api.expenses.expenses.services.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class CalculateBalanceService {
    private final MovementRepository movementRepository;
    private final UserService userService;
    private final CurrencyRepository currencyRepository;

    public Map<BalanceEnum, BigDecimal> getBalance(BalanceFilterRecord balanceFilterRecord) {
        var user = userService.getAuthenticatedUser();
        var currencies = currencyRepository.findAllBySymbol(balanceFilterRecord.currencies());
        var ingresos = movementRepository.getBalanceByFilters(
                balanceFilterRecord.year(),
                balanceFilterRecord.month(),
                user.getEmail(),
                List.of(MovementType.INGRESO.toString()),
                balanceFilterRecord.groups(),
                currencies);

        var movements = movementRepository.getBalanceByFilters(balanceFilterRecord.year(),
                balanceFilterRecord.month(), user.getEmail(),
                List.of(MovementType.CREDITO.toString(), MovementType.DEBITO.toString()),
                balanceFilterRecord.groups(),
                currencies);

        Map<BalanceEnum, BigDecimal> result = new HashMap<>();
        result.put(BalanceEnum.INGRESO, ingresos);
        result.put(BalanceEnum.GASTO, movements);

        return result;
    }

    public Set<BalanceByCategoryRecord> getBalanceWithCategoryByYear(Integer year) {
        var user = userService.getAuthenticatedUser();
        return movementRepository.getBalanceWithCategoryByYear(year, user.getEmail());
    }
}
