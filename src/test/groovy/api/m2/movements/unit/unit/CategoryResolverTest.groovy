package api.m2.movements.unit.unit

import api.m2.movements.entities.commons.Category
import api.m2.movements.records.categories.CategoryUpdateRecord
import api.m2.movements.services.category.CategoryAddService
import api.m2.movements.services.category.CategoryResolver
import api.m2.movements.services.category.WorkspaceCategoryService
import spock.lang.Specification
import spock.lang.Subject

class CategoryResolverTest extends Specification {

    CategoryAddService categoryAddService = Mock(CategoryAddService)
    WorkspaceCategoryService workspaceCategoryService = Mock(WorkspaceCategoryService)

    @Subject
    CategoryResolver service

    def setup() {
        service = new CategoryResolver(categoryAddService, workspaceCategoryService)
    }

    def "resolveAll(descriptions) - should resolve default category when descriptions is null"() {
        given:
        def defaultCategory = "Default Category"
        def workspaceId = 1L
        def category = Stub(Category) { getId() >> 1L }

        categoryAddService.getDefaultCategory() >> defaultCategory
        categoryAddService.addCategory(defaultCategory) >> category

        when:
        def result = service.resolveAll(null, workspaceId)

        then:
        1 * workspaceCategoryService.ensureCategoryInWorkspace(workspaceId, category)
        result == [category]
    }

    def "resolveAll(descriptions) - should resolve default category when descriptions is empty"() {
        given:
        def defaultCategory = "Default Category"
        def workspaceId = 1L
        def category = Stub(Category) { getId() >> 1L }

        categoryAddService.getDefaultCategory() >> defaultCategory
        categoryAddService.addCategory(defaultCategory) >> category

        when:
        def result = service.resolveAll([], workspaceId)

        then:
        1 * workspaceCategoryService.ensureCategoryInWorkspace(workspaceId, category)
        result == [category]
    }

    def "resolveAll(descriptions) - should resolve every description provided"() {
        given:
        def workspaceId = 1L
        def hogar = Stub(Category) { getId() >> 1L }
        def ocio = Stub(Category) { getId() >> 2L }

        categoryAddService.addCategory("HOGAR") >> hogar
        categoryAddService.addCategory("OCIO") >> ocio

        when:
        def result = service.resolveAll([
                new CategoryUpdateRecord(null, "HOGAR"),
                new CategoryUpdateRecord(null, "OCIO")
        ], workspaceId)

        then:
        1 * workspaceCategoryService.ensureCategoryInWorkspace(workspaceId, hogar)
        1 * workspaceCategoryService.ensureCategoryInWorkspace(workspaceId, ocio)
        result == [hogar, ocio]
    }

    def "resolveAll(records) - should resolve default category when records is null"() {
        given:
        def defaultCategory = "Default Category"
        def category = Stub(Category) { getId() >> 1L }

        categoryAddService.getDefaultCategory() >> defaultCategory
        categoryAddService.addCategory(defaultCategory) >> category

        when:
        def result = service.resolveAll(null as List<CategoryUpdateRecord>)

        then:
        result == [category]
    }

    def "resolveAll(records) - should resolve every record provided"() {
        given:
        def record = new CategoryUpdateRecord(1L, "Test Category")
        def category = Stub(Category) { getId() >> 1L }

        categoryAddService.addCategory("Test Category") >> category

        when:
        def result = service.resolveAll([record])

        then:
        result == [category]
    }
}
