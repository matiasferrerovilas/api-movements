package api.m2.movements.services.balance;

import api.m2.movements.enums.BalanceEnum;
import api.m2.movements.enums.MovementType;
import api.m2.movements.records.balance.BalanceByCategoryRecord;
import api.m2.movements.records.balance.BalanceByGroup;
import api.m2.movements.records.balance.BalanceFilterRecord;
import api.m2.movements.repositories.CurrencyRepository;
import api.m2.movements.repositories.MovementRepository;
import api.m2.movements.services.user.UserService;
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

    public Set<BalanceByCategoryRecord> getBalanceWithCategoryByYear(BalanceFilterRecord balanceFilterRecord) {
        var user = userService.getAuthenticatedUser();
        return movementRepository.getBalanceWithCategoryByYear(balanceFilterRecord.year(),
                balanceFilterRecord.month(),
                balanceFilterRecord.groups(),
                balanceFilterRecord.currencies(),
                user.getEmail());
    }

    public Set<BalanceByGroup> getBalanceByYearAndGroup(Integer year, Integer month) {
        var user = userService.getAuthenticatedUser();
        return movementRepository.getBalanceByYearAndGroup(year, month, user.getEmail());
    }
}
