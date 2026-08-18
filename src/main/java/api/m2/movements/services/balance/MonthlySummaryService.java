package api.m2.movements.services.balance;

import api.m2.movements.enums.MovementType;
import api.m2.movements.records.balance.CategoryAmountRecord;
import api.m2.movements.records.balance.MonthlySummaryByCurrencyRecord;
import api.m2.movements.records.balance.MonthlySummaryComparisonRecord;
import api.m2.movements.records.balance.MonthlySummaryResponse;
import api.m2.movements.records.balance.MonthlySummaryUnifiedRecord;
import api.m2.movements.repositories.MovementRepository;
import api.m2.movements.repositories.WorkspaceCurrencyRepository;
import api.m2.movements.services.user.UserService;
import api.m2.movements.services.workspaces.WorkspaceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MonthlySummaryService {

    private final MovementRepository movementRepository;
    private final WorkspaceCurrencyRepository workspaceCurrencyRepository;
    private final UserService userService;
    private final MonthlySummarySnapshotService snapshotService;
    private final WorkspaceQueryService workspaceQueryService;

    /**
     * Obtiene el resumen mensual de un workspace específico.
     * Verifica que el usuario autenticado sea miembro del workspace.
     */
    public MonthlySummaryResponse getSummary(Long workspaceId, Integer year, Integer month) {
        Long userId = userService.getMe().id();
        workspaceQueryService.verifyUserIsMemberOfWorkspace(workspaceId, userId);
        return snapshotService.find(workspaceId, year, month)
                .orElseGet(() -> this.computeSummary(workspaceId, year, month));
    }

    public MonthlySummaryResponse computeSummary(Long workspaceId, Integer year, Integer month) {
        YearMonth prev = YearMonth.of(year, month).minusMonths(1);
        int prevYear = prev.getYear();
        int prevMonth = prev.getMonthValue();

        List<String> currencies = workspaceCurrencyRepository.findByWorkspaceId(workspaceId).stream()
                .map(workspaceCurrency -> workspaceCurrency.getCurrency().getSymbol())
                .toList();

        List<MonthlySummaryByCurrencyRecord> porMoneda = currencies.stream()
                .map(currency -> this.buildCurrencySummary(workspaceId, year, month, prevYear, prevMonth, currency))
                .toList();

        MonthlySummaryUnifiedRecord totalUnificadoUSD =
                this.buildUnifiedUsd(workspaceId, year, month, prevYear, prevMonth);

        return new MonthlySummaryResponse(year, month, totalUnificadoUSD, porMoneda);
    }

    private MonthlySummaryByCurrencyRecord buildCurrencySummary(Long workspaceId, int year, int month,
                                                                 int prevYear, int prevMonth, String currency) {
        BigDecimal ingresado = this.getTotalByCurrency(workspaceId, year, month, MovementType.INGRESO, currency);
        BigDecimal gastado = this.getTotalByCurrency(workspaceId, year, month, MovementType.DEBITO, currency);
        String topCategory = movementRepository.getTopCategoryByMonth(workspaceId, year, month, currency).orElse(null);
        List<CategoryAmountRecord> gastosPorCategoria =
                movementRepository.getCategoryTotalsByMonth(workspaceId, year, month, currency);

        BigDecimal ingresadoAnterior =
                this.getTotalByCurrency(workspaceId, prevYear, prevMonth, MovementType.INGRESO, currency);
        BigDecimal gastadoAnterior =
                this.getTotalByCurrency(workspaceId, prevYear, prevMonth, MovementType.DEBITO, currency);

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
                ),
                gastosPorCategoria
        );
    }

    private MonthlySummaryUnifiedRecord buildUnifiedUsd(Long workspaceId, int year, int month,
                                                         int prevYear, int prevMonth) {
        BigDecimal ingresado = this.getTotalInUsd(workspaceId, year, month, MovementType.INGRESO);
        BigDecimal gastado = this.getTotalInUsd(workspaceId, year, month, MovementType.DEBITO);

        BigDecimal ingresadoAnterior = this.getTotalInUsd(workspaceId, prevYear, prevMonth, MovementType.INGRESO);
        BigDecimal gastadoAnterior = this.getTotalInUsd(workspaceId, prevYear, prevMonth, MovementType.DEBITO);

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

    private BigDecimal getTotalByCurrency(Long workspaceId, int year, int month, MovementType type, String currency) {
        return movementRepository.getTotalByTypeAndMonth(workspaceId, year, month, type.name(), currency);
    }

    private BigDecimal getTotalInUsd(Long workspaceId, int year, int month, MovementType type) {
        return movementRepository.getTotalInUsdByTypeAndMonth(workspaceId, year, month, type.name());
    }
}
