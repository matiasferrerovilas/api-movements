package api.m2.movements.movements.services.balance;

import api.m2.movements.movements.enums.MovementType;
import api.m2.movements.movements.records.balance.MonthlySummaryByCurrencyRecord;
import api.m2.movements.movements.records.balance.MonthlySummaryComparisonRecord;
import api.m2.movements.movements.records.balance.MonthlySummaryResponse;
import api.m2.movements.movements.records.balance.MonthlySummaryUnifiedRecord;
import api.m2.movements.movements.repositories.MovementRepository;
import api.m2.movements.identity.services.user.UserService;
import api.m2.movements.identity.services.workspaces.WorkspaceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MonthlySummaryService {

    private final MovementRepository movementRepository;
    private final UserService userService;
    private final MonthlySummarySnapshotService snapshotService;
    private final WorkspaceQueryService workspaceQueryService;

    /**
     * Obtiene el resumen mensual de un workspace específico.
     * Verifica que el usuario autenticado sea miembro del workspace.
     */
    public MonthlySummaryResponse getSummary(Long workspaceId, Integer year, Integer month) {
        Long userId = userService.getAuthenticatedUser().id();
        workspaceQueryService.verifyUserIsMemberOfWorkspace(workspaceId, userId);
        return snapshotService.find(userId, year, month)
                .orElseGet(() -> this.computeSummary(userId, year, month));
    }

    public MonthlySummaryResponse computeSummary(Long userId, Integer year, Integer month) {
        YearMonth prev = YearMonth.of(year, month).minusMonths(1);
        int prevYear = prev.getYear();
        int prevMonth = prev.getMonthValue();

        List<String> currencies = movementRepository
                .findDistinctCurrenciesByMonth(userId, year, month, prevYear, prevMonth);

        List<MonthlySummaryByCurrencyRecord> porMoneda = currencies.stream()
                .map(currency -> this.buildCurrencySummary(userId, year, month, prevYear, prevMonth, currency))
                .toList();

        MonthlySummaryUnifiedRecord totalUnificadoUSD = this.buildUnifiedUsd(userId, year, month, prevYear, prevMonth);

        return new MonthlySummaryResponse(year, month, totalUnificadoUSD, porMoneda);
    }

    private MonthlySummaryByCurrencyRecord buildCurrencySummary(Long userId, int year, int month,
                                                                 int prevYear, int prevMonth, String currency) {
        BigDecimal ingresado = this.getTotalByCurrency(userId, year, month, MovementType.INGRESO, currency);
        BigDecimal gastado = this.getTotalByCurrency(userId, year, month, MovementType.DEBITO, currency);
        String topCategory = movementRepository.getTopCategoryByMonth(userId, year, month, currency).orElse(null);

        BigDecimal ingresadoAnterior = this.getTotalByCurrency(userId, prevYear, prevMonth, MovementType.INGRESO, currency);
        BigDecimal gastadoAnterior = this.getTotalByCurrency(userId, prevYear, prevMonth, MovementType.DEBITO, currency);

        return new MonthlySummaryByCurrencyRecord(
                currency,
                ingresado,
                gastado,
                ingresado.subtract(gastado),
                topCategory,
                new MonthlySummaryComparisonRecord(
                        ingresadoAnterior,
                        gastadoAnterior,
                        gastado.subtract(gastadoAnterior),
                        ingresado.subtract(ingresadoAnterior)
                )
        );
    }

    private MonthlySummaryUnifiedRecord buildUnifiedUsd(Long userId, int year, int month,
                                                         int prevYear, int prevMonth) {
        BigDecimal ingresado = this.getTotalInUsd(userId, year, month, MovementType.INGRESO);
        BigDecimal gastado = this.getTotalInUsd(userId, year, month, MovementType.DEBITO);

        BigDecimal ingresadoAnterior = this.getTotalInUsd(userId, prevYear, prevMonth, MovementType.INGRESO);
        BigDecimal gastadoAnterior = this.getTotalInUsd(userId, prevYear, prevMonth, MovementType.DEBITO);

        return new MonthlySummaryUnifiedRecord(
                ingresado,
                gastado,
                ingresado.subtract(gastado),
                new MonthlySummaryComparisonRecord(
                        ingresadoAnterior,
                        gastadoAnterior,
                        gastado.subtract(gastadoAnterior),
                        ingresado.subtract(ingresadoAnterior)
                )
        );
    }

    private BigDecimal getTotalByCurrency(Long userId, int year, int month, MovementType type, String currency) {
        return movementRepository.getTotalByTypeAndMonth(userId, year, month, type.name(), currency);
    }

    private BigDecimal getTotalInUsd(Long userId, int year, int month, MovementType type) {
        return movementRepository.getTotalInUsdByTypeAndMonth(userId, year, month, type.name());
    }
}
