package api.m2.movements.unit.services

import api.m2.movements.entities.Category
import api.m2.movements.entities.Movement
import api.m2.movements.exceptions.BusinessException
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.records.categories.CategoryMigrateRequest
import api.m2.movements.repositories.CategoryRepository
import api.m2.movements.repositories.MovementRepository
import api.m2.movements.services.category.CategoryMigrateService
import api.m2.movements.services.workspaces.WorkspaceContextService
import spock.lang.Specification

class CategoryMigrateServiceTest extends Specification {

    MovementRepository movementRepository = Mock(MovementRepository)
    CategoryRepository categoryRepository = Mock(CategoryRepository)
    WorkspaceContextService workspaceContextService = Mock(WorkspaceContextService)

    CategoryMigrateService service

    def setup() {
        service = new CategoryMigrateService(movementRepository, categoryRepository, workspaceContextService)
    }

    def "migrateCategory - should reassign category to all matching workspace movements"() {
        given:
        def workspaceId = 10L
        def request = new CategoryMigrateRequest(1L, 2L)
        def toCategory = Stub(Category) { getId() >> 2L }
        def movement1 = Mock(Movement)
        def movement2 = Mock(Movement)

        workspaceContextService.getActiveWorkspaceId() >> workspaceId
        categoryRepository.findById(2L) >> Optional.of(toCategory)
        movementRepository.findByWorkspaceIdAndCategoryId(workspaceId, 1L) >> [movement1, movement2]

        when:
        service.migrateCategory(request)

        then:
        1 * movement1.setCategory(toCategory)
        1 * movement2.setCategory(toCategory)
        1 * movementRepository.saveAll([movement1, movement2])
    }

    def "migrateCategory - should throw BusinessException when fromId equals toId"() {
        given:
        def request = new CategoryMigrateRequest(5L, 5L)

        when:
        service.migrateCategory(request)

        then:
        thrown(BusinessException)
        0 * movementRepository._
    }

    def "migrateCategory - should throw EntityNotFoundException when toCategoryId does not exist"() {
        given:
        def workspaceId = 10L
        def request = new CategoryMigrateRequest(1L, 99L)

        workspaceContextService.getActiveWorkspaceId() >> workspaceId
        categoryRepository.findById(99L) >> Optional.empty()

        when:
        service.migrateCategory(request)

        then:
        thrown(EntityNotFoundException)
        0 * movementRepository.findByWorkspaceIdAndCategoryId(_ as Long, _ as Long)
    }

    def "migrateCategory - should do nothing when no movements match fromCategoryId"() {
        given:
        def workspaceId = 10L
        def request = new CategoryMigrateRequest(1L, 2L)
        def toCategory = Stub(Category) { getId() >> 2L }

        workspaceContextService.getActiveWorkspaceId() >> workspaceId
        categoryRepository.findById(2L) >> Optional.of(toCategory)
        movementRepository.findByWorkspaceIdAndCategoryId(workspaceId, 1L) >> []

        when:
        service.migrateCategory(request)

        then:
        1 * movementRepository.saveAll([])
    }
}
