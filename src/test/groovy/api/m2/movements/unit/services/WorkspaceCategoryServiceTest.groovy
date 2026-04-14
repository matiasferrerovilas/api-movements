package api.m2.movements.unit.services

import api.m2.movements.entities.Category
import api.m2.movements.entities.Workspace
import api.m2.movements.entities.WorkspaceCategory
import api.m2.movements.enums.DefaultCategory
import api.m2.movements.exceptions.BusinessException
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.exceptions.PermissionDeniedException
import api.m2.movements.mappers.WorkspaceCategoryMapper
import api.m2.movements.records.categories.CategoryRecord
import api.m2.movements.repositories.WorkspaceCategoryRepository
import api.m2.movements.services.category.CategoryAddService
import api.m2.movements.services.category.WorkspaceCategoryService
import api.m2.movements.services.workspaces.WorkspaceContextService
import spock.lang.Specification

class WorkspaceCategoryServiceTest extends Specification {

    WorkspaceCategoryRepository workspaceCategoryRepository = Mock(WorkspaceCategoryRepository)
    CategoryAddService categoryAddService = Mock(CategoryAddService)
    WorkspaceCategoryMapper workspaceCategoryMapper = Mock(WorkspaceCategoryMapper)
    WorkspaceContextService workspaceContextService = Mock(WorkspaceContextService)

    WorkspaceCategoryService service

    def setup() {
        service = new WorkspaceCategoryService(
                workspaceCategoryRepository,
                categoryAddService,
                workspaceCategoryMapper,
                workspaceContextService
        )
    }

    def "getActiveCategories - should return categories for workspace"() {
        given:
        def workspaceId = 1L
        def workspaceCategory = Stub(WorkspaceCategory)
        def categoryRecord = new CategoryRecord(1L, "COMIDA", true, true)

        workspaceContextService.getActiveWorkspaceId() >> workspaceId
        workspaceCategoryMapper.toRecordList(_ as List) >> [categoryRecord]
        workspaceCategoryRepository.findByWorkspaceIdAndIsActiveTrue(workspaceId) >> [workspaceCategory]

        when:
        def result = service.getActiveCategories()

        then:
        result.size() == 1
        result[0].description() == "COMIDA"
    }

    def "addCategory - should create category and associate to workspace"() {
        given:
        def workspaceId = 1L
        def description = "NUEVA_CATEGORIA"
        def workspace = Stub(Workspace) { getId() >> workspaceId }
        def category = Stub(Category) { getId() >> 5L }
        def workspaceCategory = Stub(WorkspaceCategory)
        def categoryRecord = new CategoryRecord(1L, description, true, true)

        workspaceContextService.getActiveWorkspace() >> workspace
        categoryAddService.addCategory(description) >> category
        workspaceCategoryRepository.findByWorkspaceIdAndCategoryId(workspaceId, 5L) >> Optional.empty()
        workspaceCategoryRepository.save(_ as WorkspaceCategory) >> workspaceCategory
        workspaceCategoryMapper.toRecord(workspaceCategory) >> categoryRecord

        when:
        def result = service.addCategory(description)

        then:
        result.description() == description
    }

    def "addCategory - should reactivate existing inactive category"() {
        given:
        def workspaceId = 1L
        def description = "CATEGORIA_INACTIVA"
        def workspace = Stub(Workspace) { getId() >> workspaceId }
        def category = Stub(Category) { getId() >> 5L }
        def existingWsCategory = Mock(WorkspaceCategory) {
            isActive() >> false
        }
        def categoryRecord = new CategoryRecord(1L, description, true, true)

        workspaceContextService.getActiveWorkspace() >> workspace
        categoryAddService.addCategory(description) >> category
        workspaceCategoryRepository.findByWorkspaceIdAndCategoryId(workspaceId, 5L) >> Optional.of(existingWsCategory)
        workspaceCategoryRepository.save(existingWsCategory) >> existingWsCategory
        workspaceCategoryMapper.toRecord(existingWsCategory) >> categoryRecord

        when:
        def result = service.addCategory(description)

        then:
        1 * existingWsCategory.setActive(true)
        result.description() == description
    }

    def "addDefaultCategories - should add SERVICIOS category to workspace"() {
        given:
        def workspace = Stub(Workspace) { getId() >> 1L }
        def category = Stub(Category) { getId() >> 5L }

        categoryAddService.addCategory(DefaultCategory.SERVICIOS.getDescription()) >> category
        workspaceCategoryRepository.findByWorkspaceIdAndCategoryId(1L, 5L) >> Optional.empty()

        when:
        service.addDefaultCategories(workspace)

        then:
        1 * workspaceCategoryRepository.save(_ as WorkspaceCategory)
    }

    def "addCategories - should add multiple categories to workspace"() {
        given:
        def workspace = Stub(Workspace) { getId() >> 1L }
        def category1 = Stub(Category) { getId() >> 5L }
        def category2 = Stub(Category) { getId() >> 6L }
        def descriptions = ["CAT1", "CAT2"]

        categoryAddService.addCategory("CAT1") >> category1
        categoryAddService.addCategory("CAT2") >> category2
        workspaceCategoryRepository.findByWorkspaceIdAndCategoryId(1L, 5L) >> Optional.empty()
        workspaceCategoryRepository.findByWorkspaceIdAndCategoryId(1L, 6L) >> Optional.empty()

        when:
        service.addCategories(workspace, descriptions)

        then:
        2 * workspaceCategoryRepository.save(_ as WorkspaceCategory)
    }

    def "deleteCategory - should delete category from workspace"() {
        given:
        def workspaceId = 1L
        def workspaceCategoryId = 10L
        def workspace = Stub(Workspace) { getId() >> workspaceId }
        def category = Stub(Category) { isDeletable() >> true }
        def workspaceCategory = Stub(WorkspaceCategory) {
            getWorkspace() >> workspace
            getCategory() >> category
        }

        workspaceContextService.getActiveWorkspaceId() >> workspaceId
        workspaceCategoryRepository.findById(workspaceCategoryId) >> Optional.of(workspaceCategory)

        when:
        service.deleteCategory(workspaceCategoryId)

        then:
        1 * workspaceCategoryRepository.delete(workspaceCategory)
    }

    def "deleteCategory - should throw EntityNotFoundException when category not found"() {
        given:
        def workspaceId = 1L
        def workspaceCategoryId = 999L

        workspaceContextService.getActiveWorkspaceId() >> workspaceId
        workspaceCategoryRepository.findById(workspaceCategoryId) >> Optional.empty()

        when:
        service.deleteCategory(workspaceCategoryId)

        then:
        thrown(EntityNotFoundException)
    }

    def "deleteCategory - should throw PermissionDeniedException when category belongs to different workspace"() {
        given:
        def workspaceId = 1L
        def workspaceCategoryId = 10L
        def differentWorkspace = Stub(Workspace) { getId() >> 999L }
        def workspaceCategory = Stub(WorkspaceCategory) {
            getWorkspace() >> differentWorkspace
        }

        workspaceContextService.getActiveWorkspaceId() >> workspaceId
        workspaceCategoryRepository.findById(workspaceCategoryId) >> Optional.of(workspaceCategory)

        when:
        service.deleteCategory(workspaceCategoryId)

        then:
        thrown(PermissionDeniedException)
    }

    def "deleteCategory - should throw BusinessException when category is not deletable"() {
        given:
        def workspaceId = 1L
        def workspaceCategoryId = 10L
        def workspace = Stub(Workspace) { getId() >> workspaceId }
        def category = Stub(Category) { isDeletable() >> false }
        def workspaceCategory = Stub(WorkspaceCategory) {
            getWorkspace() >> workspace
            getCategory() >> category
        }

        workspaceContextService.getActiveWorkspaceId() >> workspaceId
        workspaceCategoryRepository.findById(workspaceCategoryId) >> Optional.of(workspaceCategory)

        when:
        service.deleteCategory(workspaceCategoryId)

        then:
        thrown(BusinessException)
    }

    def "ensureCategoryInWorkspace - should create association if not exists"() {
        given:
        def workspace = Stub(Workspace) { getId() >> 1L }
        def category = Stub(Category) { getId() >> 5L }

        workspaceCategoryRepository.findByWorkspaceIdAndCategoryId(1L, 5L) >> Optional.empty()

        when:
        service.ensureCategoryInWorkspace(workspace, category)

        then:
        1 * workspaceCategoryRepository.save(_ as WorkspaceCategory)
    }

    def "ensureCategoryInWorkspace - should not duplicate if already exists"() {
        given:
        def workspace = Stub(Workspace) { getId() >> 1L }
        def category = Stub(Category) { getId() >> 5L }
        def existingWsCategory = Stub(WorkspaceCategory) { isActive() >> true }

        workspaceCategoryRepository.findByWorkspaceIdAndCategoryId(1L, 5L) >> Optional.of(existingWsCategory)

        when:
        service.ensureCategoryInWorkspace(workspace, category)

        then:
        0 * workspaceCategoryRepository.save(_ as WorkspaceCategory)
    }
}
