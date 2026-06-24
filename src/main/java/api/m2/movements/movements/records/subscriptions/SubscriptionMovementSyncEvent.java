package api.m2.movements.movements.records.subscriptions;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionMovementSyncEvent(
        String oldDescription,
        Long workspaceId,
        int year,
        int month,
        BigDecimal newAmount,
        String newDescription,
        LocalDate newDate) {
}
