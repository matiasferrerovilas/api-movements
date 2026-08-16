package api.m2.movements.unit.services

import api.m2.movements.enums.EventType
import api.m2.movements.events.InvitationReceivedEvent
import api.m2.movements.services.publishing.websockets.InvitationPublishServiceWebSocket
import org.springframework.messaging.simp.SimpMessagingTemplate
import spock.lang.Specification

import java.time.LocalDateTime

class InvitationPublishServiceWebSocketTest extends Specification {

    SimpMessagingTemplate messagingTemplate = Mock(SimpMessagingTemplate)
    InvitationPublishServiceWebSocket service

    def setup() {
        service = new InvitationPublishServiceWebSocket(messagingTemplate)
    }

    def "onInvitationReceived - publishes to the invited user's email-scoped topic"() {
        given:
        def event = new InvitationReceivedEvent(1L, 10L, "Casa", "owner@example.com", "invited@example.com", LocalDateTime.now())

        when:
        service.onInvitationReceived(event)

        then:
        1 * messagingTemplate.convertAndSend("/topic/invitations/invited@example.com/new", _)
    }

    def "onInvitationReceived - wraps the event with INVITATION_ADDED eventType"() {
        given:
        def event = new InvitationReceivedEvent(2L, 11L, "Oficina", "boss@example.com", "worker@example.com", LocalDateTime.now())

        when:
        service.onInvitationReceived(event)

        then:
        1 * messagingTemplate.convertAndSend(_, { wrapper ->
            wrapper.eventType() == EventType.INVITATION_ADDED &&
            wrapper.message() == event
        })
    }
}
