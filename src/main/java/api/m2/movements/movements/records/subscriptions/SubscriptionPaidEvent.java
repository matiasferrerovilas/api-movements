package api.m2.movements.movements.records.subscriptions;

import api.m2.movements.identity.entities.User;
import api.m2.movements.identity.entities.Workspace;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionPaidEvent(
        BigDecimal amount,
        LocalDate paymentDate,
        String description,
        String currencySymbol,
        User owner,
        Workspace workspace) {
}
