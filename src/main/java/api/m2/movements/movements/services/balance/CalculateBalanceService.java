package api.m2.movements.movements.services.balance;

import api.m2.movements.movements.enums.BalanceEnum;
import api.m2.movements.movements.enums.MovementType;
import api.m2.movements.movements.mappers.BalanceEvolutionMapper;
import api.m2.movements.movements.records.balance.BalanceByCategoryRecord;
import api.m2.movements.movements.records.balance.BalanceByGroup;
import api.m2.movements.movements.records.balance.BalanceFilterRecord;
import api.m2.movements.movements.records.balance.BalanceMonthlyEvolutionRecord;
import api.m2.movements.movements.repositories.CurrencyRepository;
import api.m2.movements.movements.repositories.MovementRepository;
import api.m2.movements.identity.services.user.UserService;
import api.m2.movements.identity.services.workspaces.WorkspaceContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
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
    private final BalanceEvolutionMapper balanceEvolutionMapper;
    private final WorkspaceContextService workspaceContextService;

    @Transactional(readOnly = true)
    public Map<BalanceEnum, BigDecimal> getBalance(BalanceFilterRecord balanceFilterRecord) {
        var userId = userService.getAuthenticatedUser().id();
        var workspaceId = workspaceContextService.getActiveWorkspaceId();
        var currencies = currencyRepository.findAllBySymbol(balanceFilterRecord.currencies());

        var ingresos = movementRepository.getBalanceByFilters(
                balanceFilterRecord.startDate(),
                balanceFilterRecord.endDate(),
                userId,
                List.of(MovementType.INGRESO.toString()),
                List.of(workspaceId.intValue()),
                currencies);

        var movements = movementRepository.getBalanceByFilters(
                balanceFilterRecord.startDate(),
                balanceFilterRecord.endDate(),
                userId,
                List.of(MovementType.DEBITO.toString()),
                List.of(workspaceId.intValue()),
                currencies);

        Map<BalanceEnum, BigDecimal> result = new EnumMap<>(BalanceEnum.class);
        result.put(BalanceEnum.INGRESO, ingresos);
        result.put(BalanceEnum.GASTO, movements);

        return result;
    }

    @Transactional(readOnly = true)
    public Set<BalanceByCategoryRecord> getBalanceWithCategoryByYear(BalanceFilterRecord balanceFilterRecord) {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();
        return movementRepository.getBalanceWithCategoryByYear(
                balanceFilterRecord.startDate().getYear(),
                balanceFilterRecord.startDate().getMonthValue(),
                List.of(workspaceId.intValue()),
                balanceFilterRecord.currencies());
    }

    @Transactional(readOnly = true)
    public Set<BalanceByGroup> getBalanceByYearAndGroup(Integer year, Integer month) {
        var userId = userService.getAuthenticatedUser().id();
        return movementRepository.getBalanceByYearAndGroup(year, month, userId);
    }

    @Transactional(readOnly = true)
    public List<BalanceMonthlyEvolutionRecord> getMonthlyEvolution(Integer year) {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();
        return balanceEvolutionMapper.toRecordsWithFilledMonths(
                movementRepository.findMonthlyEvolution(year, new ArrayList<>(List.of(workspaceId)))
        );
    }
}
