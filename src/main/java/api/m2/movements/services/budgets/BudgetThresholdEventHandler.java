package api.m2.movements.services.budgets;

import api.m2.movements.entities.Budget;
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

/**
 * Detecta la primera vez que un movimiento hace que el gasto acumulado de una
 * categoría cruce el límite de su presupuesto en el período, y notifica.
 * La transición antes/después se calcula restando el propio monto del movimiento
 * recién persistido, que ya está reflejado en la consulta de gasto acumulado
 * (misma transacción, misma conexión).
 */
@Component
@RequiredArgsConstructor
public class BudgetThresholdEventHandler {

    private final BudgetRepository budgetRepository;
    private final NotificationService notificationService;

    @EventListener
    @Transactional
    public void onMovementAdded(MovementRecord record) {
        if (MovementType.INGRESO.name().equals(record.type())) {
            return;
        }
        var workspaceId = record.metadata().workspace().id();
        var year = record.date().getYear();
        var month = record.date().getMonthValue();

        record.categories().forEach(category ->
                budgetRepository.findByWorkspaceCategoryDescriptionAndCurrency(
                                workspaceId, category.description(), record.currency().symbol())
                        .filter(budget -> this.matchesPeriod(budget, year, month))
                        .ifPresent(budget -> this.checkThreshold(budget, record, year, month)));
    }

    private void checkThreshold(Budget budget, MovementRecord record, int year, int month) {
        var spentAfter = this.resolveSpent(budget, year, month);
        var spentBefore = spentAfter.subtract(record.amount());
        if (spentBefore.compareTo(budget.getAmount()) < 0 && spentAfter.compareTo(budget.getAmount()) >= 0) {
            notificationService.publish(budget.getWorkspaceId(), "Presupuesto superado",
                    budget.getCategory().getDescription() + " — $" + spentAfter + "/$" + budget.getAmount(),
                    NotificationSeverity.WARNING);
        }
    }

    private boolean matchesPeriod(Budget budget, int year, int month) {
        if (budget.getYear() == null) {
            return true;
        }
        if (budget.getMonth() == null) {
            return budget.getYear() == year;
        }
        return budget.getYear() == year && budget.getMonth() == month;
    }

    private BigDecimal resolveSpent(Budget budget, int year, int month) {
        var categoryDescription = budget.getCategory().getDescription();
        var currencySymbol = budget.getCurrency().getSymbol();
        BigDecimal result = (budget.getYear() != null && budget.getMonth() == null)
                ? budgetRepository.sumSpentByCategoryAndYear(
                        budget.getWorkspaceId(), categoryDescription, currencySymbol, budget.getYear())
                : budgetRepository.sumSpentByCategoryAndPeriod(
                        budget.getWorkspaceId(), categoryDescription, currencySymbol, year, month);
        return result != null ? result : BigDecimal.ZERO;
    }
}
