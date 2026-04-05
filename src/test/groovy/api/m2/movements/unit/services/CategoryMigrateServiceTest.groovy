package api.m2.movements.unit.services

import api.m2.movements.entities.Category
import api.m2.movements.entities.Movement
import api.m2.movements.entities.User
import api.m2.movements.exceptions.BusinessException
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.records.categories.CategoryMigrateRequest
import api.m2.movements.repositories.CategoryRepository
import api.m2.movements.repositories.MovementRepository
import api.m2.movements.services.category.CategoryMigrateService
import api.m2.movements.services.user.UserService
import spock.lang.Specification

class CategoryMigrateServiceTest extends Specification {

    MovementRepository movementRepository = Mock(MovementRepository)
    CategoryRepository categoryRepository = Mock(CategoryRepository)
    UserService userService = Mock(UserService)

    CategoryMigrateService service

    def setup() {
        service = new CategoryMigrateService(movementRepository, categoryRepository, userService)
    }

    def "migrateCategory - should reassign category to all matching user movements"() {
        given:
        def request = new CategoryMigrateRequest(1L, 2L)
        def user = Stub(User) { getId() >> 10L }
        def toCategory = Stub(Category) { getId() >> 2L }
        def movement1 = Mock(Movement)
        def movement2 = Mock(Movement)

        userService.getAuthenticatedUser() >> user
        categoryRepository.findById(2L) >> Optional.of(toCategory)
        movementRepository.findByOwnerIdAndCategoryId(10L, 1L) >> [movement1, movement2]

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
        0 * userService.getAuthenticatedUser()
        0 * movementRepository._
    }

    def "migrateCategory - should throw EntityNotFoundException when toCategoryId does not exist"() {
        given:
        def request = new CategoryMigrateRequest(1L, 99L)
        def user = Stub(User) { getId() >> 10L }

        userService.getAuthenticatedUser() >> user
        categoryRepository.findById(99L) >> Optional.empty()

        when:
        service.migrateCategory(request)

        then:
        thrown(EntityNotFoundException)
        0 * movementRepository.findByOwnerIdAndCategoryId(_ as Long, _ as Long)
    }

    def "migrateCategory - should do nothing when no movements match fromCategoryId"() {
        given:
        def request = new CategoryMigrateRequest(1L, 2L)
        def user = Stub(User) { getId() >> 10L }
        def toCategory = Stub(Category) { getId() >> 2L }

        userService.getAuthenticatedUser() >> user
        categoryRepository.findById(2L) >> Optional.of(toCategory)
        movementRepository.findByOwnerIdAndCategoryId(10L, 1L) >> []

        when:
        service.migrateCategory(request)

        then:
        1 * movementRepository.saveAll([])
    }
}
