package api.m2.movements.unit.services

import api.m2.movements.entities.WorkspaceCategory
import api.m2.movements.entities.commons.Category
import api.m2.movements.enums.DefaultCategory
import api.m2.movements.repositories.WorkspaceCategoryRepository
import api.m2.movements.services.category.CategoryAddService
import api.m2.movements.services.category.WorkspaceCategoryAssociationService
import spock.lang.Specification

class WorkspaceCategoryAssociationServiceTest extends Specification {

    WorkspaceCategoryRepository workspaceCategoryRepository = Mock(WorkspaceCategoryRepository)
    CategoryAddService categoryAddService = Mock(CategoryAddService)

    WorkspaceCategoryAssociationService service

    def setup() {
        service = new WorkspaceCategoryAssociationService(workspaceCategoryRepository, categoryAddService)
    }

    def "addCategory - should create the category and associate it to the workspace"() {
        given:
        def description = "NUEVA_CATEGORIA"
        def category = Stub(Category) { getId() >> 5L }
        def workspaceCategory = Stub(WorkspaceCategory)

        categoryAddService.addCategory(description) >> category
        workspaceCategoryRepository.findByWorkspaceIdAndCategoryId(1L, 5L) >> Optional.empty()
        workspaceCategoryRepository.save(_ as WorkspaceCategory) >> workspaceCategory

        when:
        def result = service.addCategory(1L, description)

        then:
        result.is(workspaceCategory)
    }

    def "addCategory - should reactivate an existing inactive association"() {
        given:
        def description = "CATEGORIA_INACTIVA"
        def category = Stub(Category) { getId() >> 5L }
        def existingWsCategory = Mock(WorkspaceCategory) {
            isActive() >> false
        }

        categoryAddService.addCategory(description) >> category
        workspaceCategoryRepository.findByWorkspaceIdAndCategoryId(1L, 5L) >> Optional.of(existingWsCategory)
        workspaceCategoryRepository.save(existingWsCategory) >> existingWsCategory

        when:
        def result = service.addCategory(1L, description)

        then:
        1 * existingWsCategory.setActive(true)
        result.is(existingWsCategory)
    }

    def "addDefaultCategories - should add every default category except SIN_CATEGORIA to the workspace"() {
        given:
        def expectedCount = DefaultCategory.values().length - 1 // todas menos SIN_CATEGORIA
        def category = Stub(Category) { getId() >> 5L }

        workspaceCategoryRepository.findByWorkspaceIdAndCategoryId(1L, 5L) >> Optional.empty()

        when:
        service.addDefaultCategories(1L)

        then:
        // Stub + conteo de invocaciones en el mismo bloque then: — si el stub queda en given: y
        // la interacción en then: sin valor de retorno, Spock prioriza la interacción de then:
        // (que devuelve null) por sobre el stub de given:, rompiendo todo lo que dependa del
        // valor devuelto. Ya nos pasó antes en esta misma suite con este mismo patrón.
        expectedCount * categoryAddService.addCategory(_ as String) >> category
        expectedCount * workspaceCategoryRepository.save(_ as WorkspaceCategory)
    }

    def "addDefaultCategories - should never add SIN_CATEGORIA up front"() {
        given:
        def category = Stub(Category) { getId() >> 5L }

        categoryAddService.addCategory(_ as String) >> category
        workspaceCategoryRepository.findByWorkspaceIdAndCategoryId(1L, 5L) >> Optional.empty()

        when:
        service.addDefaultCategories(1L)

        then:
        0 * categoryAddService.addCategory(DefaultCategory.SIN_CATEGORIA.getDescription())
    }

    def "addCategories - should add multiple categories to workspace"() {
        given:
        def category1 = Stub(Category) { getId() >> 5L }
        def category2 = Stub(Category) { getId() >> 6L }
        def descriptions = ["CAT1", "CAT2"]

        categoryAddService.addCategory("CAT1") >> category1
        categoryAddService.addCategory("CAT2") >> category2
        workspaceCategoryRepository.findByWorkspaceIdAndCategoryId(1L, 5L) >> Optional.empty()
        workspaceCategoryRepository.findByWorkspaceIdAndCategoryId(1L, 6L) >> Optional.empty()

        when:
        service.addCategories(1L, descriptions)

        then:
        2 * workspaceCategoryRepository.save(_ as WorkspaceCategory)
    }

    def "ensureCategoryInWorkspace - should create association if not exists"() {
        given:
        def category = Stub(Category) { getId() >> 5L }

        workspaceCategoryRepository.findByWorkspaceIdAndCategoryId(1L, 5L) >> Optional.empty()

        when:
        service.ensureCategoryInWorkspace(1L, category)

        then:
        1 * workspaceCategoryRepository.save(_ as WorkspaceCategory)
    }

    def "ensureCategoryInWorkspace - should not duplicate if already exists"() {
        given:
        def category = Stub(Category) { getId() >> 5L }
        def existingWsCategory = Stub(WorkspaceCategory) { isActive() >> true }

        workspaceCategoryRepository.findByWorkspaceIdAndCategoryId(1L, 5L) >> Optional.of(existingWsCategory)

        when:
        service.ensureCategoryInWorkspace(1L, category)

        then:
        0 * workspaceCategoryRepository.save(_ as WorkspaceCategory)
    }
}
