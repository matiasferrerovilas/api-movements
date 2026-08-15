package api.m2.movements.unit.services

import api.m2.movements.enums.EventType
import api.m2.movements.enums.NotificationSeverity
import api.m2.movements.records.notifications.NotificationEvent
import api.m2.movements.records.notifications.NotificationRecord
import api.m2.movements.services.publishing.websockets.NotificationPublishServiceWebSocket
import org.springframework.messaging.simp.SimpMessagingTemplate
import spock.lang.Specification

import java.time.LocalDateTime

class NotificationPublishServiceWebSocketTest extends Specification {

    SimpMessagingTemplate messagingTemplate = Mock(SimpMessagingTemplate)
    NotificationPublishServiceWebSocket service

    def setup() {
        service = new NotificationPublishServiceWebSocket(messagingTemplate)
    }

    def "publishNotification - should publish to the workspace notifications topic"() {
        given:
        def notification = new NotificationRecord(
                "abc-123", "Presupuesto superado", "Comida — \$45000/\$40000",
                NotificationSeverity.WARNING, LocalDateTime.now())
        def event = new NotificationEvent(4L, notification)

        when:
        service.publishNotification(event)

        then:
        1 * messagingTemplate.convertAndSend("/topic/notifications/4/new", _)
    }

    def "publishNotification - should wrap the notification with NOTIFICATION_NEW eventType"() {
        given:
        def notification = new NotificationRecord(
                "abc-123", "Servicio pagado", "Netflix — \$10.00",
                NotificationSeverity.SUCCESS, LocalDateTime.now())
        def event = new NotificationEvent(9L, notification)

        when:
        service.publishNotification(event)

        then:
        1 * messagingTemplate.convertAndSend(_, { wrapper ->
            wrapper.eventType() == EventType.NOTIFICATION_NEW &&
            wrapper.message() == notification
        })
    }
}
