package api.m2.movements.records.subscriptions;

import api.m2.movements.entities.integrity.User;
import api.m2.movements.entities.integrity.Workspace;

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
