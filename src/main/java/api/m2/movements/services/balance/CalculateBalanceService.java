package api.m2.movements.services.balance;

import api.m2.movements.enums.BalanceEnum;
import api.m2.movements.enums.MovementType;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.mappers.BalanceEvolutionMapper;
import api.m2.movements.records.balance.BalanceByCategoryRecord;
import api.m2.movements.records.balance.BalanceFilterRecord;
import api.m2.movements.records.balance.BalanceMonthlyEvolutionRecord;
import api.m2.movements.records.balance.RecoveryTimeRecord;
import api.m2.movements.repositories.CurrencyRepository;
import api.m2.movements.repositories.MovementRepository;
import api.m2.movements.services.user.UserService;
import api.m2.movements.services.workspaces.WorkspaceContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class CalculateBalanceService {
    private static final int SCALE = 2;
    // El gasto incluye compras en cuotas de tarjeta (CREDITO), no solo débito directo — igual
    // que el gráfico de evolución mensual (MovementRepository#findMonthlyEvolution).
    private static final List<String> GASTO_TYPES = List.of(MovementType.DEBITO.name(), MovementType.CREDITO.name());

    private final MovementRepository movementRepository;
    private final UserService userService;
    private final CurrencyRepository currencyRepository;
    private final BalanceEvolutionMapper balanceEvolutionMapper;
    private final WorkspaceContextService workspaceContextService;

    @Transactional(readOnly = true)
    public Map<BalanceEnum, BigDecimal> getBalance(BalanceFilterRecord balanceFilterRecord) {
        var userId = userService.getMe().id();
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
                // Antes solo contaba DEBITO acá — el resto de esta clase (getMonthlyEvolution,
                // calculateRecoveryTime) y MonthlySummaryService ya incluían CREDITO como gasto;
                // esta era la única cuenta que no, dando un total de "gasto" distinto según qué
                // pantalla mirabas para el mismo período.
                GASTO_TYPES,
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
    public List<BalanceMonthlyEvolutionRecord> getMonthlyEvolution(Integer year) {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();
        return balanceEvolutionMapper.toRecordsWithFilledMonths(
                movementRepository.findMonthlyEvolution(year, new ArrayList<>(List.of(workspaceId)))
        );
    }

    /**
     * Calcula cuántos meses tomaría recuperar un gasto de {@code amount}, dado el ahorro
     * promedio (ingresos - gastos) de los últimos {@code months} meses cerrados en esa moneda.
     */
    @Transactional(readOnly = true)
    public RecoveryTimeRecord calculateRecoveryTime(BigDecimal amount, String currencySymbol, Integer months) {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();
        var normalizedSymbol = currencySymbol.trim().toUpperCase();
        currencyRepository.findBySymbol(normalizedSymbol)
                .orElseThrow(() -> new EntityNotFoundException("Moneda no encontrada: " + normalizedSymbol));

        var lastClosedMonth = YearMonth.from(LocalDate.now(ZoneOffset.UTC)).minusMonths(1);

        var totalSavings = BigDecimal.ZERO;
        for (int i = 0; i < months; i++) {
            var yearMonth = lastClosedMonth.minusMonths(i);
            var ingresado = movementRepository.getTotalByTypeAndMonth(
                    workspaceId, yearMonth.getYear(), yearMonth.getMonthValue(),
                    MovementType.INGRESO.name(), normalizedSymbol);
            var gastado = movementRepository.getTotalByTypesAndMonth(
                    workspaceId, yearMonth.getYear(), yearMonth.getMonthValue(),
                    GASTO_TYPES, normalizedSymbol);
            totalSavings = totalSavings.add(ingresado.subtract(gastado));
        }

        var averageMonthlySavings = totalSavings.divide(BigDecimal.valueOf(months), SCALE, RoundingMode.HALF_UP);

        if (averageMonthlySavings.compareTo(BigDecimal.ZERO) <= 0) {
            return new RecoveryTimeRecord(amount, normalizedSymbol, months, averageMonthlySavings, null, false);
        }

        var monthsToRecover = amount.divide(averageMonthlySavings, SCALE, RoundingMode.HALF_UP);

        return new RecoveryTimeRecord(amount, normalizedSymbol, months, averageMonthlySavings, monthsToRecover, true);
    }
}
