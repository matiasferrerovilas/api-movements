package api.m2.movements.unit.services

import api.m2.movements.enums.EventType
import api.m2.movements.records.movements.MovementDeletedEvent
import api.m2.movements.records.movements.MovementRecord
import api.m2.movements.records.workspaces.WorkspaceBaseRecord
import api.m2.movements.services.publishing.websockets.MovementPublishServiceWebSocket
import org.springframework.messaging.simp.SimpMessagingTemplate
import spock.lang.Specification

class MovementPublishServiceWebSocketTest extends Specification {

    SimpMessagingTemplate messagingTemplate = Mock(SimpMessagingTemplate)
    MovementPublishServiceWebSocket service

    def setup() {
        service = new MovementPublishServiceWebSocket(messagingTemplate)
    }

    def "publishMovementAdded - should publish to correct topic with MOVEMENT_ADDED event"() {
        given:
        def metadata = Stub(MovementRecord.Metadata) {
            workspace() >> new WorkspaceBaseRecord(1L, "Familia")
        }
        def movementRecord = Stub(MovementRecord) {
            metadata() >> metadata
        }

        when:
        service.publishMovementAdded(movementRecord)

        then:
        1 * messagingTemplate.convertAndSend("/topic/movimientos/1/new", _)
    }

    def "publishDeleteOfMovement - should publish to correct topic with MOVEMENT_DELETED event"() {
        given:
        def event = new MovementDeletedEvent(42L, 1L)

        when:
        service.publishDeleteOfMovement(event)

        then:
        1 * messagingTemplate.convertAndSend("/topic/movimientos/1/delete", _)
    }

    def "publishMovementAdded - should publish EventWrapper with correct eventType"() {
        given:
        def metadata = Stub(MovementRecord.Metadata) {
            workspace() >> new WorkspaceBaseRecord(5L, "Familia")
        }
        def movementRecord = Stub(MovementRecord) {
            metadata() >> metadata
        }

        when:
        service.publishMovementAdded(movementRecord)

        then:
        1 * messagingTemplate.convertAndSend(_, { wrapper ->
            wrapper.eventType() == EventType.MOVEMENT_ADDED &&
            wrapper.message() == movementRecord
        })
    }

    def "publishDeleteOfMovement - should publish EventWrapper with correct eventType and movementId"() {
        given:
        def event = new MovementDeletedEvent(99L, 3L)

        when:
        service.publishDeleteOfMovement(event)

        then:
        1 * messagingTemplate.convertAndSend(_, { wrapper ->
            wrapper.eventType() == EventType.MOVEMENT_DELETED &&
            wrapper.message() == 99L
        })
    }
}
