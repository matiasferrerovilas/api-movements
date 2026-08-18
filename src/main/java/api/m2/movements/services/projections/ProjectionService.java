package api.m2.movements.services.projections;

import api.m2.movements.enums.MovementType;
import api.m2.movements.enums.UserSettingKey;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.exceptions.ExchangeRateNotFoundException;
import api.m2.movements.records.projections.ProjectedPointRecord;
import api.m2.movements.records.projections.ProjectionResponse;
import api.m2.movements.repositories.MovementRepository;
import api.m2.movements.repositories.WorkspaceCurrencyRepository;
import api.m2.movements.services.currencies.ExchangeRateResolver;
import api.m2.movements.services.settings.UserSettingService;
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
 * (ingresos - gastos) de los últimos meses cerrados, expresado en la moneda por defecto
 * del usuario (property {@link UserSettingKey#DEFAULT_CURRENCY}), o USD si no configuró una.
 * Los movimientos que ya están en esa moneda se suman directo, sin ninguna conversión ni
 * pérdida de precisión; solo los movimientos en una moneda distinta pasan por el pivote USD
 * (mismo mecanismo que {@code MonthlySummaryService}) y se convierten a la moneda objetivo
 * con la tasa del día. Si la tasa de cambio no está disponible, cae a USD como fallback en
 * vez de fallar el request. Estimación conservadora de tendencia de flujo de caja: no
 * incorpora rendimientos de inversión ni ningún supuesto especulativo.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectionService {

    private static final int DEFAULT_TRAILING_MONTHS = 6;
    private static final int SCALE = 2;
    private static final String USD = "USD";
    private static final List<Integer> PROJECTION_HORIZONS_MONTHS = List.of(0, 3, 6, 12);

    private final MovementRepository movementRepository;
    private final WorkspaceQueryService workspaceQueryService;
    private final UserService userService;
    private final UserSettingService userSettingService;
    private final WorkspaceCurrencyRepository workspaceCurrencyRepository;
    private final ExchangeRateResolver exchangeRateResolver;
    private final Clock clock;

    public ProjectionResponse getProjection(Long workspaceId) {
        return this.getProjection(workspaceId, DEFAULT_TRAILING_MONTHS);
    }

    public ProjectionResponse getProjection(Long workspaceId, Integer trailingMonths) {
        Long userId = userService.getMe().id();
        workspaceQueryService.verifyUserIsMemberOfWorkspace(workspaceId, userId);

        int months = trailingMonths != null && trailingMonths > 0 ? trailingMonths : DEFAULT_TRAILING_MONTHS;

        String targetCurrency = this.resolveTargetCurrency();
        BigDecimal conversionRate = this.resolveConversionRate(targetCurrency);
        String effectiveCurrency = conversionRate != null ? targetCurrency : USD;

        BigDecimal currentBalance = this.computeCurrentBalance(workspaceId, effectiveCurrency, conversionRate);
        BigDecimal averageMonthlyNet =
                this.computeAverageMonthlyNet(workspaceId, months, effectiveCurrency, conversionRate);

        List<ProjectedPointRecord> projectedPoints = PROJECTION_HORIZONS_MONTHS.stream()
                .map(monthsOut -> new ProjectedPointRecord(
                        monthsOut,
                        currentBalance.add(averageMonthlyNet.multiply(BigDecimal.valueOf(monthsOut)))
                                .setScale(SCALE, RoundingMode.HALF_UP)))
                .toList();

        return new ProjectionResponse(currentBalance, averageMonthlyNet, months, effectiveCurrency, projectedPoints);
    }

    /**
     * Moneda por defecto del usuario autenticado (property {@code DEFAULT_CURRENCY}). El valor
     * guardado es el id de la fila {@code WorkspaceCurrency} (la asociación workspace-moneda,
     * ver {@code WorkspaceCurrencyService.toRecord}), NO el id de {@code Currency} directamente
     * — son tablas distintas con ids independientes. Si el usuario no configuró una, cae a USD.
     */
    private String resolveTargetCurrency() {
        try {
            Long workspaceCurrencyId = userSettingService.getByKey(UserSettingKey.DEFAULT_CURRENCY).value();
            return workspaceCurrencyRepository.findByIdFetchCurrency(workspaceCurrencyId)
                    .map(wc -> wc.getCurrency().getSymbol())
                    .orElse(USD);
        } catch (EntityNotFoundException e) {
            return USD;
        }
    }

    /**
     * Cuántas unidades de {@code targetCurrency} equivalen a 1 USD, hoy — solo se usa para
     * convertir los movimientos que NO están ya en {@code targetCurrency}. {@code null} si la
     * conversión no está disponible (moneda desconocida, o falla la consulta de tasa de cambio
     * en vivo): el caller debe interpretarlo como "devolver en USD" en vez de fallar el
     * request entero por un problema transitorio de un servicio externo.
     */
    private BigDecimal resolveConversionRate(String targetCurrency) {
        if (USD.equalsIgnoreCase(targetCurrency)) {
            return BigDecimal.ONE;
        }
        try {
            return exchangeRateResolver.resolveRate(targetCurrency, LocalDate.now(clock));
        } catch (ExchangeRateNotFoundException e) {
            log.warn("No se pudo obtener la tasa de cambio a {}, se devuelve la proyección en USD: {}",
                    targetCurrency, e.getMessage());
            return null;
        }
    }

    /**
     * Balance actual: ahorro neto acumulado (ingresos - gastos) de todo el historial de
     * movimientos del workspace, expresado en {@code targetCurrency}. Es la mejor aproximación
     * disponible a un "balance" ya que esta aplicación no modela cuentas bancarias con saldo
     * propio.
     */
    private BigDecimal computeCurrentBalance(Long workspaceId, String targetCurrency, BigDecimal conversionRate) {
        BigDecimal totalIngresado = this.getTotalInTargetCurrency(
                workspaceId, MovementType.INGRESO.name(), targetCurrency, conversionRate);
        BigDecimal totalGastado = this.getTotalInTargetCurrency(
                workspaceId, MovementType.DEBITO.name(), targetCurrency, conversionRate);
        return totalIngresado.subtract(totalGastado).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal computeAverageMonthlyNet(Long workspaceId, int trailingMonths,
                                                 String targetCurrency, BigDecimal conversionRate) {
        YearMonth lastClosedMonth = YearMonth.from(LocalDate.now(clock)).minusMonths(1);

        BigDecimal totalNet = BigDecimal.ZERO;
        for (int i = 0; i < trailingMonths; i++) {
            YearMonth yearMonth = lastClosedMonth.minusMonths(i);
            BigDecimal ingresado = this.getTotalInTargetCurrencyByMonth(
                    workspaceId, yearMonth, MovementType.INGRESO.name(), targetCurrency, conversionRate);
            BigDecimal gastado = this.getTotalInTargetCurrencyByMonth(
                    workspaceId, yearMonth, MovementType.DEBITO.name(), targetCurrency, conversionRate);
            totalNet = totalNet.add(ingresado.subtract(gastado));
        }

        return totalNet.divide(BigDecimal.valueOf(trailingMonths), SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Suma en {@code targetCurrency}, para todo el historial: los movimientos que ya están en
     * esa moneda se suman directo (sin ninguna conversión); el resto pasa por el pivote USD y
     * se convierte con {@code conversionRate}. Si {@code targetCurrency} es USD, todo pasa por
     * el camino existente de siempre (equivalente, ya que dividir por una tasa de 1 no cambia
     * nada).
     */
    private BigDecimal getTotalInTargetCurrency(Long workspaceId, String type,
                                                 String targetCurrency, BigDecimal conversionRate) {
        if (USD.equalsIgnoreCase(targetCurrency)) {
            return movementRepository.getTotalInUsdByType(workspaceId, type);
        }
        BigDecimal nativeTotal = movementRepository.getTotalByType(workspaceId, type, targetCurrency);
        BigDecimal otherCurrenciesUsdTotal =
                movementRepository.getTotalInUsdByTypeExcludingCurrency(workspaceId, type, targetCurrency);
        return nativeTotal.add(otherCurrenciesUsdTotal.multiply(conversionRate));
    }

    /** Igual que {@link #getTotalInTargetCurrency}, pero acotado a un mes puntual. */
    private BigDecimal getTotalInTargetCurrencyByMonth(Long workspaceId, YearMonth yearMonth, String type,
                                                         String targetCurrency, BigDecimal conversionRate) {
        int year = yearMonth.getYear();
        int month = yearMonth.getMonthValue();
        if (USD.equalsIgnoreCase(targetCurrency)) {
            return movementRepository.getTotalInUsdByTypeAndMonth(workspaceId, year, month, type);
        }
        BigDecimal nativeTotal =
                movementRepository.getTotalByTypeAndMonth(workspaceId, year, month, type, targetCurrency);
        BigDecimal otherCurrenciesUsdTotal = movementRepository.getTotalInUsdByTypeAndMonthExcludingCurrency(
                workspaceId, year, month, type, targetCurrency);
        return nativeTotal.add(otherCurrenciesUsdTotal.multiply(conversionRate));
    }
}
