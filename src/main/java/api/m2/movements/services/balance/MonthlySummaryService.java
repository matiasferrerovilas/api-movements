package api.m2.movements.services.balance;

import api.m2.movements.enums.MovementType;
import api.m2.movements.projections.MonthlyUserCurrencyProjection;
import api.m2.movements.records.balance.CategoryAmountRecord;
import api.m2.movements.records.balance.MonthlySummaryByCurrencyRecord;
import api.m2.movements.records.balance.MonthlySummaryComparisonRecord;
import api.m2.movements.records.balance.MonthlySummaryResponse;
import api.m2.movements.records.balance.MonthlySummaryUnifiedRecord;
import api.m2.movements.records.balance.MonthlySummaryUserRecord;
import api.m2.movements.repositories.MovementRepository;
import api.m2.movements.repositories.WorkspaceCurrencyRepository;
import api.m2.movements.services.user.UserService;
import api.m2.movements.services.workspaces.WorkspaceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonthlySummaryService {

    private final MovementRepository movementRepository;
    private final WorkspaceCurrencyRepository workspaceCurrencyRepository;
    private final UserService userService;
    private final WorkspaceQueryService workspaceQueryService;

    /**
     * Resumen mensual de un workspace. Siempre se calcula con SUM contra {@code movements} en el
     * momento de la consulta — no hay cache/snapshot: el volumen es chico y un snapshot quedaba
     * desactualizado en cuanto se cargaba un movimiento después de generarlo.
     * Verifica que el usuario autenticado sea miembro del workspace.
     */
    public MonthlySummaryResponse getSummary(Long workspaceId, Integer year, Integer month) {
        Long userId = userService.getMe().id();
        workspaceQueryService.verifyUserIsMemberOfWorkspace(workspaceId, userId);
        return this.computeSummary(workspaceId, year, month);
    }

    /**
     * Desglose "por usuario" del cierre de mes: cada miembro del workspace con al menos un
     * movimiento ese mes, cuántos cargó y cuánto de gasto suman, por moneda. Endpoint aparte del
     * resumen (lo consume solo la pestaña "Por usuario") para no meter una llamada a api-identity
     * en el camino de {@code getSummary}, que también usa InsightService.
     */
    public List<MonthlySummaryUserRecord> getUserBreakdown(Long workspaceId, Integer year, Integer month) {
        Long callerId = userService.getMe().id();
        workspaceQueryService.verifyUserIsMemberOfWorkspace(workspaceId, callerId);

        List<MonthlyUserCurrencyProjection> rows =
                movementRepository.getUserTotalsByMonth(workspaceId, year, month);
        if (rows.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = rows.stream().map(MonthlyUserCurrencyProjection::getUserId).distinct().toList();
        Map<Long, String> namesById = userService.getUserDisplayNamesByIds(userIds);

        Map<Long, List<MonthlySummaryUserRecord.PerCurrency>> byUser = rows.stream()
                .collect(Collectors.groupingBy(
                        MonthlyUserCurrencyProjection::getUserId,
                        Collectors.mapping(row -> new MonthlySummaryUserRecord.PerCurrency(
                                row.getCurrency(), row.getMovementCount(), row.getTotalSpent()), Collectors.toList())));

        return byUser.entrySet().stream()
                .map(entry -> new MonthlySummaryUserRecord(
                        entry.getKey(),
                        namesById.getOrDefault(entry.getKey(), "#" + entry.getKey()),
                        entry.getValue().stream()
                                .sorted(Comparator.comparing(MonthlySummaryUserRecord.PerCurrency::currency))
                                .toList()))
                .sorted(Comparator.comparing(MonthlySummaryUserRecord::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public MonthlySummaryResponse computeSummary(Long workspaceId, Integer year, Integer month) {
        YearMonth prev = YearMonth.of(year, month).minusMonths(1);
        int prevYear = prev.getYear();
        int prevMonth = prev.getMonthValue();

        List<String> currencies = workspaceCurrencyRepository.findByWorkspaceId(workspaceId).stream()
                .map(workspaceCurrency -> workspaceCurrency.getCurrency().getSymbol())
                .toList();

        List<MonthlySummaryByCurrencyRecord> perCurrency = currencies.stream()
                .map(currency -> this.buildCurrencySummary(workspaceId, year, month, prevYear, prevMonth, currency))
                .toList();

        MonthlySummaryUnifiedRecord totalUsd =
                this.buildUnifiedUsd(workspaceId, year, month, prevYear, prevMonth);

        return new MonthlySummaryResponse(year, month, totalUsd, perCurrency);
    }

    private MonthlySummaryByCurrencyRecord buildCurrencySummary(Long workspaceId, int year, int month,
                                                                 int prevYear, int prevMonth, String currency) {
        long movementCount = movementRepository.countByMonthAndCurrency(workspaceId, year, month, currency);
        BigDecimal income = this.getTotalByCurrency(workspaceId, year, month, MovementType.INGRESO, currency);
        // El gasto incluye compras en cuotas de tarjeta (CREDITO), no solo débito directo. Se pide
        // cada tipo por separado para poder mostrar el desglose en el cierre de mes; la suma es
        // el mismo total que antes daba getTotalByTypesAndMonth([DEBITO, CREDITO]).
        BigDecimal spentDebit = this.getTotalByCurrency(workspaceId, year, month, MovementType.DEBITO, currency);
        BigDecimal spentCredit = this.getTotalByCurrency(workspaceId, year, month, MovementType.CREDITO, currency);
        BigDecimal spent = spentDebit.add(spentCredit);
        String topCategory = movementRepository.getTopCategoryByMonth(workspaceId, year, month, currency).orElse(null);
        List<CategoryAmountRecord> spendingByCategory =
                movementRepository.getCategoryTotalsByMonth(workspaceId, year, month, currency);

        BigDecimal prevIncome =
                this.getTotalByCurrency(workspaceId, prevYear, prevMonth, MovementType.INGRESO, currency);
        BigDecimal prevSpent = this.getSpentByCurrency(workspaceId, prevYear, prevMonth, currency);

        return new MonthlySummaryByCurrencyRecord(
                currency,
                movementCount,
                income,
                spent,
                spentDebit,
                spentCredit,
                income.subtract(spent),
                topCategory,
                new MonthlySummaryComparisonRecord(
                        prevIncome,
                        prevSpent,
                        spent.subtract(prevSpent),
                        income.subtract(prevIncome)
                ),
                spendingByCategory
        );
    }

    private MonthlySummaryUnifiedRecord buildUnifiedUsd(Long workspaceId, int year, int month,
                                                         int prevYear, int prevMonth) {
        BigDecimal income = this.getTotalInUsd(workspaceId, year, month, MovementType.INGRESO);
        BigDecimal spentDebit = this.getTotalInUsd(workspaceId, year, month, MovementType.DEBITO);
        BigDecimal spentCredit = this.getTotalInUsd(workspaceId, year, month, MovementType.CREDITO);
        BigDecimal spent = spentDebit.add(spentCredit);

        BigDecimal prevIncome = this.getTotalInUsd(workspaceId, prevYear, prevMonth, MovementType.INGRESO);
        BigDecimal prevSpent = this.getSpentInUsd(workspaceId, prevYear, prevMonth);

        return new MonthlySummaryUnifiedRecord(
                income,
                spent,
                spentDebit,
                spentCredit,
                income.subtract(spent),
                new MonthlySummaryComparisonRecord(
                        prevIncome,
                        prevSpent,
                        spent.subtract(prevSpent),
                        income.subtract(prevIncome)
                )
        );
    }

    private BigDecimal getTotalByCurrency(Long workspaceId, int year, int month, MovementType type, String currency) {
        return movementRepository.getTotalByTypeAndMonth(workspaceId, year, month, type.name(), currency);
    }

    private BigDecimal getTotalInUsd(Long workspaceId, int year, int month, MovementType type) {
        return movementRepository.getTotalInUsdByTypeAndMonth(workspaceId, year, month, type.name());
    }

    private BigDecimal getSpentByCurrency(Long workspaceId, int year, int month, String currency) {
        return this.getTotalByCurrency(workspaceId, year, month, MovementType.DEBITO, currency)
                .add(this.getTotalByCurrency(workspaceId, year, month, MovementType.CREDITO, currency));
    }

    private BigDecimal getSpentInUsd(Long workspaceId, int year, int month) {
        return this.getTotalInUsd(workspaceId, year, month, MovementType.DEBITO)
                .add(this.getTotalInUsd(workspaceId, year, month, MovementType.CREDITO));
    }
}
