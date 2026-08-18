package api.m2.movements.services.projections;

import api.m2.movements.enums.MovementType;
import api.m2.movements.records.projections.ProjectedPointRecord;
import api.m2.movements.records.projections.ProjectionResponse;
import api.m2.movements.repositories.MovementRepository;
import api.m2.movements.services.user.UserService;
import api.m2.movements.services.workspaces.WorkspaceQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Proyecta el balance futuro extrapolando linealmente el ahorro neto promedio
 * (ingresos - gastos, unificado en USD) de los últimos meses cerrados. Estimación
 * conservadora de tendencia de flujo de caja: no incorpora rendimientos de inversión
 * ni ningún supuesto especulativo.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectionService {

    private static final int DEFAULT_TRAILING_MONTHS = 6;
    private static final int SCALE = 2;
    private static final List<Integer> PROJECTION_HORIZONS_MONTHS = List.of(0, 3, 6, 12);

    private final MovementRepository movementRepository;
    private final WorkspaceQueryService workspaceQueryService;
    private final UserService userService;
    private final Clock clock;

    public ProjectionResponse getProjection(Long workspaceId) {
        return this.getProjection(workspaceId, DEFAULT_TRAILING_MONTHS);
    }

    public ProjectionResponse getProjection(Long workspaceId, Integer trailingMonths) {
        Long userId = userService.getMe().id();
        workspaceQueryService.verifyUserIsMemberOfWorkspace(workspaceId, userId);

        int months = trailingMonths != null && trailingMonths > 0 ? trailingMonths : DEFAULT_TRAILING_MONTHS;

        BigDecimal currentBalance = this.computeCurrentBalance(workspaceId);
        BigDecimal averageMonthlyNet = this.computeAverageMonthlyNet(workspaceId, months);

        List<ProjectedPointRecord> projectedPoints = PROJECTION_HORIZONS_MONTHS.stream()
                .map(monthsOut -> new ProjectedPointRecord(
                        monthsOut,
                        currentBalance.add(averageMonthlyNet.multiply(BigDecimal.valueOf(monthsOut)))
                                .setScale(SCALE, RoundingMode.HALF_UP)))
                .toList();

        return new ProjectionResponse(currentBalance, averageMonthlyNet, months, projectedPoints);
    }

    /**
     * Balance actual: ahorro neto acumulado (ingresos - gastos) de todo el historial
     * de movimientos del workspace, unificado en USD. Es la mejor aproximación disponible
     * a un "balance" ya que esta aplicación no modela cuentas bancarias con saldo propio.
     */
    private BigDecimal computeCurrentBalance(Long workspaceId) {
        BigDecimal totalIngresado = movementRepository.getTotalInUsdByType(workspaceId, MovementType.INGRESO.name());
        BigDecimal totalGastado = movementRepository.getTotalInUsdByType(workspaceId, MovementType.DEBITO.name());
        return totalIngresado.subtract(totalGastado).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal computeAverageMonthlyNet(Long workspaceId, int trailingMonths) {
        YearMonth lastClosedMonth = YearMonth.from(LocalDate.now(clock)).minusMonths(1);

        BigDecimal totalNet = BigDecimal.ZERO;
        for (int i = 0; i < trailingMonths; i++) {
            YearMonth yearMonth = lastClosedMonth.minusMonths(i);
            BigDecimal ingresado = movementRepository.getTotalInUsdByTypeAndMonth(
                    workspaceId, yearMonth.getYear(), yearMonth.getMonthValue(), MovementType.INGRESO.name());
            BigDecimal gastado = movementRepository.getTotalInUsdByTypeAndMonth(
                    workspaceId, yearMonth.getYear(), yearMonth.getMonthValue(), MovementType.DEBITO.name());
            totalNet = totalNet.add(ingresado.subtract(gastado));
        }

        return totalNet.divide(BigDecimal.valueOf(trailingMonths), SCALE, RoundingMode.HALF_UP);
    }
}
