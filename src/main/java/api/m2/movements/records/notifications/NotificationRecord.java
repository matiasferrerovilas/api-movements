package api.m2.movements.records.notifications;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import api.m2.movements.enums.NotificationSeverity;

import java.time.LocalDateTime;

@RegisterReflectionForBinding
public record NotificationRecord(String id,
                                  String title,
                                  String message,
                                  NotificationSeverity severity,
                                  LocalDateTime createdAt) {
}
