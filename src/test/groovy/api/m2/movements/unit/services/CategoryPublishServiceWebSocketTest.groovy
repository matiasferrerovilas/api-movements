package api.m2.movements.unit.services

import api.m2.movements.enums.EventType
import api.m2.movements.records.categories.CategoryRecord
import api.m2.movements.records.categories.CategoryUpdatedEvent
import api.m2.movements.services.publishing.websockets.CategoryPublishServiceWebSocket
import org.springframework.messaging.simp.SimpMessagingTemplate
import spock.lang.Specification

class CategoryPublishServiceWebSocketTest extends Specification {

    SimpMessagingTemplate messagingTemplate = Mock(SimpMessagingTemplate)
    CategoryPublishServiceWebSocket service

    def setup() {
        service = new CategoryPublishServiceWebSocket(messagingTemplate)
    }

    def "publishCategoryUpdated - should publish to correct topic with CATEGORY_UPDATED event"() {
        given:
        def category = Stub(CategoryRecord)
        def event = new CategoryUpdatedEvent(category, 1L)

        when:
        service.publishCategoryUpdated(event)

        then:
        1 * messagingTemplate.convertAndSend("/topic/categories/1/update", { wrapper ->
            wrapper.eventType() == EventType.CATEGORY_UPDATED &&
            wrapper.message() == category
        })
    }

    def "publishCategoryUpdated - should use workspaceId from event"() {
        given:
        def category = Stub(CategoryRecord)
        def event = new CategoryUpdatedEvent(category, 42L)

        when:
        service.publishCategoryUpdated(event)

        then:
        1 * messagingTemplate.convertAndSend("/topic/categories/42/update", _)
    }
}
