package api.m2.movements.events;

import api.m2.movements.records.services.SubscriptionRecord;

public record ServicePaidEvent(SubscriptionRecord subscription) {
}
