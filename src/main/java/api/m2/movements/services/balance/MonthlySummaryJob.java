package api.m2.movements.services.balance;

import api.m2.movements.entities.User;
import api.m2.movements.services.user.UserService;
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

    private final UserService userService;
    private final MonthlySummaryService monthlySummaryService;
    private final MonthlySummarySnapshotService snapshotService;

    @Scheduled(cron = "0 0 23 L * *")
    public void generateMonthlySnapshots() {
        YearMonth target = YearMonth.now();
        int year = target.getYear();
        int month = target.getMonthValue();

        List<User> users = userService.getUsersWithMonthlySnapshotEnabled();
        log.info("Generando snapshots mensuales para {}/{} — {} usuarios", month, year, users.size());

        users.forEach(user -> {
            var summary = monthlySummaryService.computeSummary(user.getEmail(), year, month);
            snapshotService.save(user, year, month, summary);
        });
    }
}
