package api.m2.movements.unit.services

import api.m2.movements.enums.EventType
import api.m2.movements.records.services.SubscriptionRecord
import api.m2.movements.services.publishing.websockets.ServicePublishServiceWebSocket
import org.springframework.messaging.simp.SimpMessagingTemplate
import spock.lang.Specification

class ServicePublishServiceWebSocketTest extends Specification {

    SimpMessagingTemplate messagingTemplate = Mock(SimpMessagingTemplate)
    ServicePublishServiceWebSocket service

    def setup() {
        service = new ServicePublishServiceWebSocket(messagingTemplate)
    }

    def "publishServicePaid - should publish to update topic with SERVICE_PAID event"() {
        given:
        def subscription = Stub(SubscriptionRecord) {
            workspaceId() >> 1L
        }

        when:
        service.publishServicePaid(subscription)

        then:
        1 * messagingTemplate.convertAndSend("/topic/servicios/1/update", { wrapper ->
            wrapper.eventType() == EventType.SERVICE_PAID
        })
    }

    def "publishUpdateService - should publish to update topic with SERVICE_UPDATED event"() {
        given:
        def subscription = Stub(SubscriptionRecord) {
            workspaceId() >> 2L
        }

        when:
        service.publishUpdateService(subscription)

        then:
        1 * messagingTemplate.convertAndSend("/topic/servicios/2/update", { wrapper ->
            wrapper.eventType() == EventType.SERVICE_UPDATED
        })
    }

    def "publishNewService - should publish to new topic with SERVICE_PAID event"() {
        given:
        def subscription = Stub(SubscriptionRecord) {
            workspaceId() >> 3L
        }

        when:
        service.publishNewService(subscription)

        then:
        1 * messagingTemplate.convertAndSend("/topic/servicios/3/new", { wrapper ->
            wrapper.eventType() == EventType.SERVICE_PAID
        })
    }

    def "publishDeleteService - should publish to remove topic with SERVICE_DELETED event"() {
        given:
        def subscription = Stub(SubscriptionRecord) {
            workspaceId() >> 4L
        }

        when:
        service.publishDeleteService(subscription)

        then:
        1 * messagingTemplate.convertAndSend("/topic/servicios/4/remove", { wrapper ->
            wrapper.eventType() == EventType.SERVICE_DELETED
        })
    }
}
