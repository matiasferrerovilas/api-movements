package api.m2.movements.events;

import api.m2.movements.records.services.SubscriptionRecord;

public record ServiceUpdatedEvent(SubscriptionRecord subscription) {
}
