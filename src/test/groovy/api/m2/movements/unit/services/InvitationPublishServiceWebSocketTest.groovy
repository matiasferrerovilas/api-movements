package api.m2.movements.unit.services

import api.m2.movements.enums.EventType
import api.m2.movements.enums.InvitationStatus
import api.m2.movements.events.InvitationAcceptedReceivedEvent
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

    def "onInvitationReceived - wraps a WorkspaceInvitationDTO (matching the REST shape) with INVITATION_ADDED eventType"() {
        given:
        def createdAt = LocalDateTime.now()
        def event = new InvitationReceivedEvent(2L, 11L, "Oficina", "boss@example.com", "worker@example.com", createdAt)

        when:
        service.onInvitationReceived(event)

        then:
        1 * messagingTemplate.convertAndSend(_, { wrapper ->
            wrapper.eventType() == EventType.INVITATION_ADDED &&
            wrapper.message().id() == event.invitationId() &&
            wrapper.message().workspaceId() == event.workspaceId() &&
            wrapper.message().workspaceName() == event.workspaceName() &&
            wrapper.message().invitedByEmail() == event.invitedByEmail() &&
            wrapper.message().status() == InvitationStatus.PENDING &&
            wrapper.message().createdAt() == createdAt
        })
    }

    def "onInvitationAccepted - publishes to the workspace's members topic"() {
        given:
        def event = new InvitationAcceptedReceivedEvent(1L, 10L, "Casa", "invited@example.com", LocalDateTime.now())

        when:
        service.onInvitationAccepted(event)

        then:
        1 * messagingTemplate.convertAndSend("/topic/workspace/10/members/update", _)
    }

    def "onInvitationAccepted - wraps the event with MEMBERSHIP_UPDATED eventType"() {
        given:
        def event = new InvitationAcceptedReceivedEvent(2L, 11L, "Oficina", "worker@example.com", LocalDateTime.now())

        when:
        service.onInvitationAccepted(event)

        then:
        1 * messagingTemplate.convertAndSend(_, { wrapper ->
            wrapper.eventType() == EventType.MEMBERSHIP_UPDATED &&
            wrapper.message() == event
        })
    }
}
