package api.m2.movements.unit.services

import api.m2.movements.movements.enums.EventType
import api.m2.movements.identity.records.workspaces.WorkspaceDetail
import api.m2.movements.identity.services.WorkspacePublishServiceWebSocket
import org.springframework.messaging.simp.SimpMessagingTemplate
import spock.lang.Specification

class WorkspacePublishServiceWebSocketTest extends Specification {

    SimpMessagingTemplate messagingTemplate = Mock(SimpMessagingTemplate)
    WorkspacePublishServiceWebSocket service

    def setup() {
        service = new WorkspacePublishServiceWebSocket(messagingTemplate)
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
}
