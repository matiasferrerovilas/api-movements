package api.m2.movements.services.balance;

import api.m2.movements.repositories.MovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class MonthlySummaryJob {

    private final MovementRepository movementRepository;
    private final MonthlySummaryService monthlySummaryService;
    private final MonthlySummarySnapshotService snapshotService;

    @Scheduled(cron = "0 0 23 L * *")
    public void generateMonthlySnapshots() {
        YearMonth target = YearMonth.now();
        int year = target.getYear();
        int month = target.getMonthValue();

        List<Long> workspaceIds = movementRepository.findDistinctWorkspaceIds();
        log.info("Generando snapshots mensuales para {}/{} — {} workspaces", month, year, workspaceIds.size());

        workspaceIds.forEach(workspaceId -> {
            var summary = monthlySummaryService.computeSummary(workspaceId, year, month);
            snapshotService.save(workspaceId, year, month, summary);
        });
    }
}
