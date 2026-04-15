package api.m2.movements.unit.services

import api.m2.movements.enums.EventType
import api.m2.movements.records.invite.InvitationToWorkspaceRecord
import api.m2.movements.records.users.UserBaseRecord
import api.m2.movements.records.workspaces.WorkspaceDetail
import api.m2.movements.records.workspaces.WorkspaceRecord
import api.m2.movements.services.publishing.websockets.WorkspacePublishServiceWebSocket
import org.springframework.messaging.simp.SimpMessagingTemplate
import spock.lang.Specification

class WorkspacePublishServiceWebSocketTest extends Specification {

    SimpMessagingTemplate messagingTemplate = Mock(SimpMessagingTemplate)
    WorkspacePublishServiceWebSocket service

    def setup() {
        service = new WorkspacePublishServiceWebSocket(messagingTemplate)
    }

    def "publishInvitationAdded - should publish to correct topic"() {
        given:
        def invitation = Stub(InvitationToWorkspaceRecord) {
            invitedUserId() >> 42L
        }

        when:
        service.publishInvitationAdded(invitation)

        then:
        1 * messagingTemplate.convertAndSend("/topic/invitation/42/new", { wrapper ->
            wrapper.eventType() == EventType.INVITATION_ADDED
        })
    }

    def "publishInvitationUpdated - should publish to correct topic"() {
        given:
        def invitation = Stub(InvitationToWorkspaceRecord) {
            invitedUserId() >> 42L
        }

        when:
        service.publishInvitationUpdated(invitation)

        then:
        1 * messagingTemplate.convertAndSend("/topic/invitation/42/update", { wrapper ->
            wrapper.eventType() == EventType.INVITATION_CONFIRMED_REJECTED
        })
    }

    def "publishWorkspaceCreated - should publish to owner topic"() {
        given:
        def workspaceRecord = Stub(WorkspaceRecord) {
            owner() >> new UserBaseRecord("Test User", 10L)
        }

        when:
        service.publishWorkspaceCreated(workspaceRecord)

        then:
        1 * messagingTemplate.convertAndSend("/topic/workspace/10/new", { wrapper ->
            wrapper.eventType() == EventType.WORKSPACE_CREATED
        })
    }

    def "publishWorkspaceLeft - should publish to workspace topic"() {
        given:
        def workspaceRecord = Stub(WorkspaceRecord) {
            id() >> 5L
        }

        when:
        service.publishWorkspaceLeft(workspaceRecord)

        then:
        1 * messagingTemplate.convertAndSend("/topic/workspace/5/leave", { wrapper ->
            wrapper.eventType() == EventType.WORKSPACE_LEFT
        })
    }

    def "publishWorkspaceMembershipUpdated - should publish to default topic with keycloak subject"() {
        given:
        def workspaceDetail = Stub(WorkspaceDetail)
        def keycloakSubject = "550e8400-e29b-41d4-a716-446655440000"

        when:
        service.publishWorkspaceMembershipUpdated(workspaceDetail, keycloakSubject)

        then:
        1 * messagingTemplate.convertAndSend("/topic/workspace/default/550e8400-e29b-41d4-a716-446655440000", { wrapper ->
            wrapper.eventType() == EventType.MEMBERSHIP_UPDATED
        })
    }

    def "publishMemberAdded - should publish to members update topic"() {
        given:
        def workspaceDetail = Stub(WorkspaceDetail)
        def workspaceId = 7L

        when:
        service.publishMemberAdded(workspaceDetail, workspaceId)

        then:
        1 * messagingTemplate.convertAndSend("/topic/workspace/7/members/update", { wrapper ->
            wrapper.eventType() == EventType.MEMBERSHIP_UPDATED
        })
    }
}
