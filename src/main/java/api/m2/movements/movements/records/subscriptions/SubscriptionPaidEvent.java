package api.m2.movements.movements.records.subscriptions;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionPaidEvent(
        BigDecimal amount,
        LocalDate paymentDate,
        String description,
        String currencySymbol,
        Long ownerId,
        Long workspaceId) {
}
