package api.m2.movements.unit.services

import api.m2.movements.enums.NotificationSeverity
import api.m2.movements.records.notifications.NotificationEvent
import api.m2.movements.services.notifications.NotificationService
import org.springframework.context.ApplicationEventPublisher
import spock.lang.Specification

class NotificationServiceTest extends Specification {

    ApplicationEventPublisher eventPublisher = Mock(ApplicationEventPublisher)
    NotificationService service

    def setup() {
        service = new NotificationService(eventPublisher)
    }

    def "publish - should publish a NotificationEvent for the given workspace"() {
        when:
        service.publish(7L, "Presupuesto superado", "Comida — \$45000/\$40000", NotificationSeverity.WARNING)

        then:
        1 * eventPublisher.publishEvent(_ as NotificationEvent) >> { List args ->
            def event = args[0] as NotificationEvent
            assert event.workspaceId() == 7L
            assert event.notification().title() == "Presupuesto superado"
            assert event.notification().message() == "Comida — \$45000/\$40000"
            assert event.notification().severity() == NotificationSeverity.WARNING
        }
    }

    def "publish - should assign a unique id and a createdAt timestamp"() {
        when:
        service.publish(1L, "Servicio pagado", "Netflix — \$10.00", NotificationSeverity.SUCCESS)

        then:
        1 * eventPublisher.publishEvent(_ as NotificationEvent) >> { List args ->
            def event = args[0] as NotificationEvent
            assert event.notification().id() != null
            assert !event.notification().id().isEmpty()
            assert event.notification().createdAt() != null
        }
    }

    def "publish - should generate a different id on each call"() {
        given:
        def ids = []

        when:
        service.publish(1L, "t", "m", NotificationSeverity.INFO)
        service.publish(1L, "t", "m", NotificationSeverity.INFO)

        then:
        2 * eventPublisher.publishEvent(_ as NotificationEvent) >> { List args ->
            ids << (args[0] as NotificationEvent).notification().id()
        }
        ids.size() == 2
        ids[0] != ids[1]
    }
}
