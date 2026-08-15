package api.m2.movements.services.notifications;

import api.m2.movements.enums.NotificationSeverity;
import api.m2.movements.records.notifications.NotificationEvent;
import api.m2.movements.records.notifications.NotificationRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final ApplicationEventPublisher eventPublisher;

    public void publish(Long workspaceId, String title, String message, NotificationSeverity severity) {
        var notification = new NotificationRecord(
                UUID.randomUUID().toString(),
                title,
                message,
                severity,
                LocalDateTime.now(ZoneOffset.UTC));
        eventPublisher.publishEvent(new NotificationEvent(workspaceId, notification));
    }
}
