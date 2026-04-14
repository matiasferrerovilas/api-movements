package api.m2.movements.unit.services

import api.m2.movements.entities.Category
import api.m2.movements.entities.User
import api.m2.movements.entities.UserCategory
import api.m2.movements.enums.DefaultCategory
import api.m2.movements.exceptions.BusinessException
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.exceptions.PermissionDeniedException
import api.m2.movements.mappers.UserCategoryMapper
import api.m2.movements.repositories.UserCategoryRepository
import api.m2.movements.services.category.CategoryAddService
import api.m2.movements.services.category.UserCategoryService
import api.m2.movements.services.user.UserService
import org.mapstruct.factory.Mappers
import spock.lang.Specification

class UserCategoryServiceTest extends Specification {

    UserCategoryRepository userCategoryRepository = Mock(UserCategoryRepository)
    CategoryAddService categoryAddService = Mock(CategoryAddService)
    UserCategoryMapper userCategoryMapper = Mappers.getMapper(UserCategoryMapper)
    UserService userService = Mock(UserService)

    UserCategoryService service

    def setup() {
        service = new UserCategoryService(
                userCategoryRepository,
                categoryAddService,
                userCategoryMapper,
                userService
        )
    }

    def "getActiveCategories - should return active categories for authenticated user"() {
        given:
        def userId = 1L
        def user = Stub(User) { getId() >> userId }
        def category = Stub(Category) { getId() >> 10L; getDescription() >> "COMIDA"; isDeletable() >> true }
        def userCategory = new UserCategory(id: 1L, user: user, category: category, active: true)

        userService.getAuthenticatedUser() >> user
        userCategoryRepository.findByUserIdAndIsActiveTrue(userId) >> [userCategory]

        when:
        def result = service.getActiveCategories()

        then:
        result.size() == 1
        result[0].description() == "COMIDA"
    }

    def "getActiveCategories - should return empty list when user has no active categories"() {
        given:
        def userId = 1L
        def user = Stub(User) { getId() >> userId }

        userService.getAuthenticatedUser() >> user
        userCategoryRepository.findByUserIdAndIsActiveTrue(userId) >> []

        when:
        def result = service.getActiveCategories()

        then:
        result.isEmpty()
    }

    def "addCategory - should create and link category to user"() {
        given:
        def userId = 1L
        def user = Stub(User) { getId() >> userId }
        def category = Stub(Category) { getId() >> 10L; getDescription() >> "TRANSPORTE"; isDeletable() >> true }
        def userCategory = new UserCategory(id: 1L, user: user, category: category, active: true)

        userService.getAuthenticatedUser() >> user
        categoryAddService.addCategory("TRANSPORTE") >> category
        userCategoryRepository.findByUserIdAndCategoryId(userId, 10L) >> Optional.empty()
        userCategoryRepository.save(_ as UserCategory) >> userCategory

        when:
        def result = service.addCategory("TRANSPORTE")

        then:
        result.description() == "TRANSPORTE"
    }

    def "addCategory - should reactivate existing inactive category"() {
        given:
        def userId = 1L
        def user = Stub(User) { getId() >> userId }
        def category = Stub(Category) { getId() >> 10L; getDescription() >> "COMIDA"; isDeletable() >> true }
        def existingUserCategory = new UserCategory(id: 1L, user: user, category: category, active: false)

        userService.getAuthenticatedUser() >> user
        categoryAddService.addCategory("COMIDA") >> category
        userCategoryRepository.findByUserIdAndCategoryId(userId, 10L) >> Optional.of(existingUserCategory)
        userCategoryRepository.save(_ as UserCategory) >> { UserCategory uc -> uc }

        when:
        def result = service.addCategory("COMIDA")

        then:
        result.description() == "COMIDA"
    }

    def "addDefaultCategories - should add SERVICIOS category"() {
        given:
        def userId = 1L
        def user = Stub(User) { getId() >> userId }
        def category = Stub(Category) { getId() >> 10L; getDescription() >> "SERVICIOS" }
        def userCategory = new UserCategory(id: 1L, user: user, category: category, active: true)

        and:
        categoryAddService.addCategory("SERVICIOS") >> category
        userCategoryRepository.findByUserIdAndCategoryId(userId, 10L) >> Optional.empty()
        userCategoryRepository.save(_ as UserCategory) >> userCategory

        when:
        service.addDefaultCategories(user)

        then:
        1 * userCategoryRepository.save(_ as UserCategory)
    }

    def "addCategories - should add multiple categories for user"() {
        given:
        def userId = 1L
        def user = Stub(User) { getId() >> userId }
        def descriptions = ["CAT1", "CAT2", "CAT3"]

        def category1 = Stub(Category) { getId() >> 1L; getDescription() >> "CAT1" }
        def category2 = Stub(Category) { getId() >> 2L; getDescription() >> "CAT2" }
        def category3 = Stub(Category) { getId() >> 3L; getDescription() >> "CAT3" }

        categoryAddService.addCategory("CAT1") >> category1
        categoryAddService.addCategory("CAT2") >> category2
        categoryAddService.addCategory("CAT3") >> category3

        userCategoryRepository.findByUserIdAndCategoryId(userId, 1L) >> Optional.empty()
        userCategoryRepository.findByUserIdAndCategoryId(userId, 2L) >> Optional.empty()
        userCategoryRepository.findByUserIdAndCategoryId(userId, 3L) >> Optional.empty()
        userCategoryRepository.save(_ as UserCategory) >> { UserCategory uc -> uc }

        when:
        service.addCategories(user, descriptions)

        then:
        3 * userCategoryRepository.save(_ as UserCategory)
    }

    def "deleteCategory - should delete deletable category owned by user"() {
        given:
        def userId = 1L
        def categoryId = 10L
        def user = Stub(User) { getId() >> userId }
        def category = Stub(Category) { getId() >> categoryId; getDescription() >> "COMIDA"; isDeletable() >> true }
        def userCategory = new UserCategory(id: 1L, user: user, category: category, active: true)

        userService.getAuthenticatedUser() >> user
        userCategoryRepository.findById(1L) >> Optional.of(userCategory)

        when:
        service.deleteCategory(1L)

        then:
        1 * userCategoryRepository.delete(userCategory)
    }

    def "deleteCategory - should throw EntityNotFoundException when category not found"() {
        given:
        def user = Stub(User) { getId() >> 1L }

        userService.getAuthenticatedUser() >> user
        userCategoryRepository.findById(999L) >> Optional.empty()

        when:
        service.deleteCategory(999L)

        then:
        thrown(EntityNotFoundException)
    }

    def "deleteCategory - should throw PermissionDeniedException when category belongs to another user"() {
        given:
        def userId = 1L
        def otherUserId = 2L
        def user = Stub(User) { getId() >> userId }
        def otherUser = Stub(User) { getId() >> otherUserId }
        def category = Stub(Category) { getId() >> 10L; getDescription() >> "COMIDA"; isDeletable() >> true }
        def userCategory = new UserCategory(id: 1L, user: otherUser, category: category, active: true)

        userService.getAuthenticatedUser() >> user
        userCategoryRepository.findById(1L) >> Optional.of(userCategory)

        when:
        service.deleteCategory(1L)

        then:
        thrown(PermissionDeniedException)
    }

    def "deleteCategory - should throw BusinessException when category is not deletable"() {
        given:
        def userId = 1L
        def user = Stub(User) { getId() >> userId }
        def category = Stub(Category) { getId() >> 10L; getDescription() >> "SERVICIOS"; isDeletable() >> false }
        def userCategory = new UserCategory(id: 1L, user: user, category: category, active: true)

        userService.getAuthenticatedUser() >> user
        userCategoryRepository.findById(1L) >> Optional.of(userCategory)

        when:
        service.deleteCategory(1L)

        then:
        thrown(BusinessException)
    }
}
