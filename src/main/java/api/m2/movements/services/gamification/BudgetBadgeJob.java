package api.m2.movements.services.gamification;

import api.m2.movements.repositories.MovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.List;

/**
 * Evaluates "budget met" badges once a month's period closes — mirrors {@code MonthlySummaryJob}'s
 * timing, since both need the month's spending to be final before computing anything.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BudgetBadgeJob {

    private final MovementRepository movementRepository;
    private final BudgetBadgeService budgetBadgeService;

    @Scheduled(cron = "0 0 23 L * *")
    public void evaluateClosedBudgets() {
        YearMonth target = YearMonth.now();
        int year = target.getYear();
        int month = target.getMonthValue();

        List<Long> workspaceIds = movementRepository.findDistinctWorkspaceIds();
        log.info("Evaluando badges de presupuesto cumplido para {}/{} — {} workspaces", month, year, workspaceIds.size());

        workspaceIds.forEach(workspaceId -> budgetBadgeService.evaluateClosedPeriod(workspaceId, year, month));
    }
}
