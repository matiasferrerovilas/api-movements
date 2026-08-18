package api.m2.movements.services.insights;

import api.m2.movements.enums.InsightDirection;
import api.m2.movements.records.balance.CategoryAmountRecord;
import api.m2.movements.records.balance.MonthlySummaryByCurrencyRecord;
import api.m2.movements.records.balance.MonthlySummaryResponse;
import api.m2.movements.records.insights.CategoryInsightRecord;
import api.m2.movements.services.balance.MonthlySummaryService;
import api.m2.movements.services.user.UserService;
import api.m2.movements.services.workspaces.WorkspaceQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Detecta anomalías de gasto: para cada categoría, compara el monto gastado en el mes actual
 * contra el promedio de los últimos N meses (calculado sobre los snapshots mensuales existentes,
 * ver {@link MonthlySummaryService}) y marca como "insight" aquellas cuya desviación supere
 * el umbral configurado.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InsightService {

    private static final int DEFAULT_TRAILING_MONTHS = 6;
    private static final BigDecimal DEVIATION_THRESHOLD_PERCENT = new BigDecimal("25");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final MonthlySummaryService monthlySummaryService;
    private final WorkspaceQueryService workspaceQueryService;
    private final UserService userService;
    private final Clock clock;

    public List<CategoryInsightRecord> getInsights(Long workspaceId) {
        return this.getInsights(workspaceId, DEFAULT_TRAILING_MONTHS);
    }

    public List<CategoryInsightRecord> getInsights(Long workspaceId, int trailingMonths) {
        Long userId = userService.getMe().id();
        workspaceQueryService.verifyUserIsMemberOfWorkspace(workspaceId, userId);

        YearMonth current = YearMonth.now(clock);
        MonthlySummaryResponse currentSummary = this.fetchSummary(workspaceId, current);

        List<MonthlySummaryResponse> history = IntStream.rangeClosed(1, trailingMonths)
                .mapToObj(current::minusMonths)
                .map(yearMonth -> this.fetchSummary(workspaceId, yearMonth))
                .toList();

        List<CategoryInsightRecord> insights = new ArrayList<>();
        for (MonthlySummaryByCurrencyRecord currentByCurrency : currentSummary.porMoneda()) {
            String currency = currentByCurrency.currency();
            List<CategoryAmountRecord> categorias = currentByCurrency.gastosPorCategoria();
            if (categorias == null) {
                continue;
            }
            for (CategoryAmountRecord categoryAmount : categorias) {
                this.buildInsight(categoryAmount, currency, history).ifPresent(insights::add);
            }
        }

        return insights.stream()
                .sorted(Comparator.comparing(
                        (CategoryInsightRecord insight) -> insight.percentDeviation().abs())
                        .reversed())
                .toList();
    }

    private MonthlySummaryResponse fetchSummary(Long workspaceId, YearMonth yearMonth) {
        return monthlySummaryService.getSummary(workspaceId, yearMonth.getYear(), yearMonth.getMonthValue());
    }

    private Optional<CategoryInsightRecord> buildInsight(CategoryAmountRecord currentCategoryAmount,
                                                           String currency,
                                                           List<MonthlySummaryResponse> history) {
        String category = currentCategoryAmount.category();
        BigDecimal current = currentCategoryAmount.amount() != null ? currentCategoryAmount.amount() : BigDecimal.ZERO;

        if (history.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal sum = history.stream()
                .map(summary -> this.findCategoryAmount(summary, currency, category))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = sum.divide(BigDecimal.valueOf(history.size()), 2, RoundingMode.HALF_UP);

        if (average.compareTo(BigDecimal.ZERO) == 0) {
            // Sin historial de gasto en esta categoría: no hay base para calcular una desviación.
            return Optional.empty();
        }

        BigDecimal percentDeviation = current.subtract(average)
                .multiply(HUNDRED)
                .divide(average, 2, RoundingMode.HALF_UP);

        if (percentDeviation.abs().compareTo(DEVIATION_THRESHOLD_PERCENT) <= 0) {
            return Optional.empty();
        }

        InsightDirection direction = percentDeviation.signum() >= 0 ? InsightDirection.ABOVE : InsightDirection.BELOW;

        return Optional.of(new CategoryInsightRecord(
                category, currency, current, average, percentDeviation.abs(), direction));
    }

    private BigDecimal findCategoryAmount(MonthlySummaryResponse summary, String currency, String category) {
        if (summary.porMoneda() == null) {
            return BigDecimal.ZERO;
        }
        return summary.porMoneda().stream()
                .filter(byCurrency -> currency.equals(byCurrency.currency()))
                .findFirst()
                .map(MonthlySummaryByCurrencyRecord::gastosPorCategoria)
                .stream()
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(categoryAmount -> category.equals(categoryAmount.category()))
                .map(CategoryAmountRecord::amount)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }
}
