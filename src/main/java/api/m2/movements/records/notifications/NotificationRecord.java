package api.m2.movements.records.notifications;

import api.m2.movements.enums.NotificationSeverity;

import java.time.LocalDateTime;

public record NotificationRecord(String id,
                                  String title,
                                  String message,
                                  NotificationSeverity severity,
                                  LocalDateTime createdAt) {
}
