package api.m2.movements.services.insights;

import api.m2.movements.enums.InsightDirection;
import api.m2.movements.enums.MovementType;
import api.m2.movements.enums.NotificationSeverity;
import api.m2.movements.records.movements.MovementRecord;
import api.m2.movements.repositories.BudgetRepository;
import api.m2.movements.services.notifications.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;

/**
 * Detecta la primera vez que un movimiento hace que la desviación de gasto de una categoría
 * (vs. su promedio de los últimos 6 meses, ver {@link InsightService}) cruce el umbral, y
 * notifica. Misma técnica de "antes/después" que {@link
 * api.m2.movements.services.budgets.BudgetThresholdEventHandler}: solo dispara en el movimiento
 * exacto que causa el cruce, sin necesidad de persistir qué ya se notificó.
 */
@Component
@RequiredArgsConstructor
public class InsightThresholdEventHandler {

    private final BudgetRepository budgetRepository;
    private final InsightService insightService;
    private final NotificationService notificationService;
    private final Clock clock;

    @EventListener
    @Transactional
    public void onMovementAdded(MovementRecord record) {
        if (MovementType.INGRESO.name().equals(record.type())) {
            return;
        }
        var workspaceId = record.metadata().workspace().id();
        var year = record.date().getYear();
        var month = record.date().getMonthValue();

        // Los insights se calculan para el mes calendario actual — un movimiento cargado con
        // fecha pasada no afecta el gasto "actual" que compara InsightService, así que no hay
        // nada que reevaluar.
        var now = YearMonth.now(clock);
        if (year != now.getYear() || month != now.getMonthValue()) {
            return;
        }

        var currency = record.currency().symbol();
        record.categories().forEach(category ->
                this.checkThreshold(workspaceId, category.description(), currency, record.amount(), year, month));
    }

    private void checkThreshold(Long workspaceId, String category, String currency, BigDecimal movementAmount,
                                 int year, int month) {
        BigDecimal spentAfter = budgetRepository.sumSpentByCategoryAndPeriod(workspaceId, category, currency,
                year, month);
        spentAfter = spentAfter != null ? spentAfter : BigDecimal.ZERO;
        BigDecimal spentBefore = spentAfter.subtract(movementAmount);

        var wasFlagged = insightService.evaluateCategory(workspaceId, category, currency, spentBefore).isPresent();
        if (wasFlagged) {
            return;
        }

        insightService.evaluateCategory(workspaceId, category, currency, spentAfter).ifPresent(insight -> {
            var sign = insight.direction() == InsightDirection.ABOVE ? "+" : "-";
            notificationService.publish(workspaceId, "Gasto fuera de lo normal",
                    category + " — " + sign + insight.percentDeviation() + "% vs. promedio de 6 meses",
                    NotificationSeverity.INFO);
        });
    }
}
