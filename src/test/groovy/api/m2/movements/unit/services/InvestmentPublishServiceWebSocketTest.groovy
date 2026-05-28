package api.m2.movements.unit.services

import api.m2.movements.enums.EventType
import api.m2.movements.records.investments.InvestmentRecord
import api.m2.movements.services.publishing.websockets.InvestmentPublishServiceWebSocket
import org.springframework.messaging.simp.SimpMessagingTemplate
import spock.lang.Specification

class InvestmentPublishServiceWebSocketTest extends Specification {

    SimpMessagingTemplate messagingTemplate = Mock(SimpMessagingTemplate)
    InvestmentPublishServiceWebSocket service

    def setup() {
        service = new InvestmentPublishServiceWebSocket(messagingTemplate)
    }

    def "publishInvestmentAdded - should publish to new topic with INVESTMENT_ADDED event"() {
        given:
        def record = Stub(InvestmentRecord) { workspaceId() >> 1L }

        when:
        service.publishInvestmentAdded(record)

        then:
        1 * messagingTemplate.convertAndSend("/topic/inversiones/1/new", { wrapper ->
            wrapper.eventType() == EventType.INVESTMENT_ADDED
        })
    }

    def "publishInvestmentUpdated - should publish to update topic with INVESTMENT_UPDATED event"() {
        given:
        def record = Stub(InvestmentRecord) { workspaceId() >> 2L }

        when:
        service.publishInvestmentUpdated(record)

        then:
        1 * messagingTemplate.convertAndSend("/topic/inversiones/2/update", { wrapper ->
            wrapper.eventType() == EventType.INVESTMENT_UPDATED
        })
    }

    def "publishInvestmentDeleted - should publish to delete topic with INVESTMENT_DELETED event"() {
        given:
        def record = Stub(InvestmentRecord) { workspaceId() >> 3L }

        when:
        service.publishInvestmentDeleted(record)

        then:
        1 * messagingTemplate.convertAndSend("/topic/inversiones/3/delete", { wrapper ->
            wrapper.eventType() == EventType.INVESTMENT_DELETED
        })
    }
}
