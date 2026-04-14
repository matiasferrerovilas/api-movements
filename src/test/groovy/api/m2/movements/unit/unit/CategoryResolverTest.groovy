package api.m2.movements.unit.unit

import api.m2.movements.entities.Category
import api.m2.movements.entities.Workspace
import api.m2.movements.records.categories.CategoryRecord
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

    def "resolve - should add category when record is null"() {
        given:
        def defaultCategory = "Default Category"
        def workspace = Stub(Workspace) { getId() >> 1L }
        def category = Stub(Category) { getId() >> 1L }

        categoryAddService.getDefaultCategory() >> defaultCategory
        categoryAddService.addCategory(defaultCategory) >> category

        when:
        def result = service.resolve(null as CategoryRecord, workspace)

        then:
        1 * workspaceCategoryService.ensureCategoryInWorkspace(workspace, category)
        result == category
    }

    def "resolve - should add category when record is not null"() {
        given:
        def description = "Test Category"
        def workspace = Stub(Workspace) { getId() >> 1L }
        def record = new CategoryRecord(1L, description, true, true)
        def category = Stub(Category) { getId() >> 1L }

        categoryAddService.addCategory(description) >> category

        when:
        def result = service.resolve(record, workspace)

        then:
        1 * workspaceCategoryService.ensureCategoryInWorkspace(workspace, category)
        result == category
    }

    def "resolve - should add category when description is provided"() {
        given:
        def description = "Test Category"
        def workspace = Stub(Workspace) { getId() >> 1L }
        def category = Stub(Category) { getId() >> 1L }

        categoryAddService.addCategory(description) >> category

        when:
        def result = service.resolve(description, workspace)

        then:
        1 * workspaceCategoryService.ensureCategoryInWorkspace(workspace, category)
        result == category
    }
}
