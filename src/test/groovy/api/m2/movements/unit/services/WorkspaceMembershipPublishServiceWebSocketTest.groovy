package api.m2.movements.unit.services

import api.m2.movements.enums.EventType
import api.m2.movements.events.MemberRemovedReceivedEvent
import api.m2.movements.services.publishing.websockets.WorkspaceMembershipPublishServiceWebSocket
import org.springframework.messaging.simp.SimpMessagingTemplate
import spock.lang.Specification

import java.time.LocalDateTime

class WorkspaceMembershipPublishServiceWebSocketTest extends Specification {

    SimpMessagingTemplate messagingTemplate = Mock(SimpMessagingTemplate)
    WorkspaceMembershipPublishServiceWebSocket service

    def setup() {
        service = new WorkspaceMembershipPublishServiceWebSocket(messagingTemplate)
    }

    def "onMemberRemoved - publishes to the removed user's email-scoped topic"() {
        given:
        def event = new MemberRemovedReceivedEvent(10L, "Casa", "owner@example.com", "removed@example.com", LocalDateTime.now())

        when:
        service.onMemberRemoved(event)

        then:
        1 * messagingTemplate.convertAndSend("/topic/membership/removed@example.com/remove", _)
    }

    def "onMemberRemoved - wraps the event with WORKSPACE_LEFT eventType"() {
        given:
        def event = new MemberRemovedReceivedEvent(11L, "Oficina", "boss@example.com", "worker@example.com", LocalDateTime.now())

        when:
        service.onMemberRemoved(event)

        then:
        1 * messagingTemplate.convertAndSend(_, { wrapper ->
            wrapper.eventType() == EventType.WORKSPACE_LEFT &&
            wrapper.message() == event
        })
    }
}
