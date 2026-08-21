package api.m2.movements.services.gamification;

import api.m2.movements.entities.Badge;
import api.m2.movements.entities.Budget;
import api.m2.movements.enums.BadgeType;
import api.m2.movements.enums.NotificationSeverity;
import api.m2.movements.records.gamification.BadgeRecord;
import api.m2.movements.repositories.BadgeRepository;
import api.m2.movements.repositories.BudgetRepository;
import api.m2.movements.services.notifications.NotificationService;
import api.m2.movements.services.workspaces.WorkspaceContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Awards a "budget met" badge for a category once its period closes — a budget can't be
 * meaningfully "cumplido" while the period is still open, since spending could still cross the
 * limit before it ends. See {@link BudgetBadgeJob} for when this runs.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BudgetBadgeService {

    private final BudgetRepository budgetRepository;
    private final BadgeRepository badgeRepository;
    private final NotificationService notificationService;
    private final WorkspaceContextService workspaceContextService;

    @Transactional
    public void evaluateClosedPeriod(Long workspaceId, int year, int month) {
        budgetRepository.findByWorkspaceAndPeriod(workspaceId, year, month).stream()
                .filter(budget -> budget.getCategory() != null)
                // Un presupuesto anual (year fijo, month null) usa un total corrido todo el año —
                // premiarlo cada mes que el corrido esté bajo el límite daría un badge redundante
                // por mes. El hábito mensual que busca esta feature ya lo cubren los presupuestos
                // "siempre activos" (year null) y los de mes específico.
                .filter(budget -> budget.getYear() == null || budget.getMonth() != null)
                .forEach(budget -> this.evaluateBudget(budget, year, month));
    }

    @Transactional(readOnly = true)
    public List<BadgeRecord> getBadges() {
        Long workspaceId = workspaceContextService.getActiveWorkspaceId();
        return badgeRepository.findByWorkspaceIdOrderByEarnedAtDesc(workspaceId).stream()
                .map(this::toRecord)
                .toList();
    }

    private void evaluateBudget(Budget budget, int year, int month) {
        Long categoryId = budget.getCategory().getId();
        if (badgeRepository.existsByWorkspaceIdAndCategoryIdAndYearAndMonthAndType(
                budget.getWorkspaceId(), categoryId, year, month, BadgeType.BUDGET_MET)) {
            return;
        }

        BigDecimal spent = this.resolveSpent(budget, year, month);
        if (spent.compareTo(budget.getAmount()) > 0) {
            return;
        }

        Badge badge = Badge.builder()
                .workspaceId(budget.getWorkspaceId())
                .category(budget.getCategory())
                .type(BadgeType.BUDGET_MET)
                .year(year)
                .month(month)
                .build();
        badgeRepository.save(badge);

        notificationService.publish(budget.getWorkspaceId(), "¡Presupuesto cumplido!",
                budget.getCategory().getDescription() + " — te mantuviste dentro del límite en " + month + "/" + year,
                NotificationSeverity.SUCCESS);
        log.info("Badge BUDGET_MET otorgado: workspace={}, categoria={}, período={}/{}",
                budget.getWorkspaceId(), categoryId, month, year);
    }

    private BigDecimal resolveSpent(Budget budget, int year, int month) {
        String categoryDescription = budget.getCategory().getDescription();
        String currencySymbol = budget.getCurrency().getSymbol();
        BigDecimal result = (budget.getYear() != null && budget.getMonth() == null)
                ? budgetRepository.sumSpentByCategoryAndYear(
                        budget.getWorkspaceId(), categoryDescription, currencySymbol, budget.getYear())
                : budgetRepository.sumSpentByCategoryAndPeriod(
                        budget.getWorkspaceId(), categoryDescription, currencySymbol, year, month);
        return result != null ? result : BigDecimal.ZERO;
    }

    private BadgeRecord toRecord(Badge badge) {
        return new BadgeRecord(
                badge.getId(),
                badge.getCategory() == null ? null : badge.getCategory().getDescription(),
                badge.getType(),
                badge.getYear(),
                badge.getMonth(),
                badge.getEarnedAt());
    }
}
