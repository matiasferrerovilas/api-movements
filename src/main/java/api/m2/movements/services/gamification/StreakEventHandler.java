package api.m2.movements.services.gamification;

import api.m2.movements.records.movements.MovementRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Feeds {@link StreakService} from every newly-created movement — {@code MovementRecord} is only
 * published on creation (see {@code MovementAddService}), never on update, so this only ever counts
 * genuine new registrations, not edits.
 */
@Component
@RequiredArgsConstructor
public class StreakEventHandler {

    private final StreakService streakService;

    @EventListener
    @Transactional
    public void onMovementAdded(MovementRecord record) {
        streakService.recordActivity(
                record.metadata().owner().id(),
                record.metadata().workspace().id(),
                record.createdAt().toLocalDate());
    }
}
