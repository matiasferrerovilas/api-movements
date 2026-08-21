package api.m2.movements.services.gamification;

import api.m2.movements.entities.UserStreak;
import api.m2.movements.records.gamification.StreakRecord;
import api.m2.movements.repositories.UserStreakRepository;
import api.m2.movements.services.user.UserService;
import api.m2.movements.services.workspaces.WorkspaceContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Simple day-streak tracking ("registraste gastos N días seguidos") — no points system, just a
 * per-user, per-workspace counter updated whenever {@link StreakEventHandler} sees a new movement.
 * Nothing is reset by a scheduled job: a broken streak is detected lazily on read (see
 * {@link #getStreak}), comparing the stored {@code lastActivityDate} against today.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StreakService {

    private final UserStreakRepository userStreakRepository;
    private final UserService userService;
    private final WorkspaceContextService workspaceContextService;

    @Transactional(readOnly = true)
    public StreakRecord getStreak() {
        Long userId = userService.getMe().id();
        Long workspaceId = workspaceContextService.getActiveWorkspaceId();

        return userStreakRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
                .map(this::toRecord)
                .orElse(new StreakRecord(0, 0, null));
    }

    @Transactional
    public void recordActivity(Long userId, Long workspaceId, LocalDate activityDate) {
        var streak = userStreakRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
                .orElseGet(() -> UserStreak.builder().userId(userId).workspaceId(workspaceId).build());

        if (streak.getLastActivityDate() != null && !activityDate.isAfter(streak.getLastActivityDate())) {
            // Ya contabilizado ese día (o un evento fuera de orden) — nada que hacer.
            return;
        }

        long gapDays = streak.getLastActivityDate() == null
                ? -1
                : ChronoUnit.DAYS.between(streak.getLastActivityDate(), activityDate);

        int newStreak = gapDays == 1 ? streak.getCurrentStreak() + 1 : 1;

        streak.setCurrentStreak(newStreak);
        streak.setLongestStreak(Math.max(streak.getLongestStreak(), newStreak));
        streak.setLastActivityDate(activityDate);

        userStreakRepository.save(streak);
        log.debug("Racha actualizada: usuario={}, workspace={}, actual={}, record={}",
                userId, workspaceId, newStreak, streak.getLongestStreak());
    }

    private StreakRecord toRecord(UserStreak streak) {
        boolean broken = streak.getLastActivityDate() == null
                || ChronoUnit.DAYS.between(streak.getLastActivityDate(), LocalDate.now()) > 1;
        int effectiveCurrent = broken ? 0 : streak.getCurrentStreak();
        return new StreakRecord(effectiveCurrent, streak.getLongestStreak(), streak.getLastActivityDate());
    }
}
