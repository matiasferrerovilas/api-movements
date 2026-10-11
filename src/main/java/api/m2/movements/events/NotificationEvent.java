package api.m2.movements.events;

import api.m2.movements.records.notifications.NotificationRecord;

public record NotificationEvent(Long workspaceId, NotificationRecord notification) {
}
