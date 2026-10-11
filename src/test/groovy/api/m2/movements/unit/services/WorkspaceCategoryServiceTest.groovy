package api.m2.movements.unit.services

import api.m2.movements.entities.commons.Category
import api.m2.movements.entities.WorkspaceCategory
import api.m2.movements.exceptions.BusinessException
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.exceptions.PermissionDeniedException
import api.m2.movements.mappers.WorkspaceCategoryMapper
import api.m2.movements.records.categories.CategoryPatchRequest
import api.m2.movements.records.categories.CategoryRecord
import api.m2.movements.records.categories.CategoryUpdatedEvent
import api.m2.movements.repositories.CategoryRepository
import api.m2.movements.repositories.WorkspaceCategoryRepository
import api.m2.movements.services.category.WorkspaceCategoryAssociationService
import api.m2.movements.services.category.WorkspaceCategoryService
import api.m2.movements.services.workspaces.WorkspaceContextService
import org.springframework.context.ApplicationEventPublisher
import spock.lang.Specification

class WorkspaceCategoryServiceTest extends Specification {

    WorkspaceCategoryRepository workspaceCategoryRepository = Mock(WorkspaceCategoryRepository)
    WorkspaceCategoryAssociationService associationService = Mock(WorkspaceCategoryAssociationService)
    WorkspaceCategoryMapper workspaceCategoryMapper = Mock(WorkspaceCategoryMapper)
    WorkspaceContextService workspaceContextService = Mock(WorkspaceContextService)
    CategoryRepository categoryRepository = Mock(CategoryRepository)
    ApplicationEventPublisher eventPublisher = Mock(ApplicationEventPublisher)

    WorkspaceCategoryService service

    def setup() {
        service = new WorkspaceCategoryService(
                workspaceCategoryRepository,
                associationService,
                workspaceCategoryMapper,
                workspaceContextService,
                categoryRepository,
                eventPublisher
        )
    }

    def "getActiveCategories - should return categories for workspace"() {
        given:
        def workspaceId = 1L
        def workspaceCategory = Stub(WorkspaceCategory)
        def categoryRecord = new CategoryRecord(1L, "COMIDA", true, true, null, null)

        workspaceContextService.getActiveWorkspaceId() >> workspaceId
        workspaceCategoryMapper.toRecordList(_ as List) >> [categoryRecord]
        workspaceCategoryRepository.findByWorkspaceIdAndIsActiveTrue(workspaceId) >> [workspaceCategory]

        when:
        def result = service.getActiveCategories()

        then:
        result.size() == 1
        result[0].description() == "COMIDA"
    }

    def "addCategory - should associate the category to the active writable workspace and map the result"() {
        given:
        def description = "NUEVA_CATEGORIA"
        def workspaceCategory = Stub(WorkspaceCategory)
        def categoryRecord = new CategoryRecord(1L, description, true, true, null, null)

        workspaceContextService.getActiveWritableWorkspaceId() >> 1L
        associationService.addCategory(1L, description) >> workspaceCategory
        workspaceCategoryMapper.toRecord(workspaceCategory) >> categoryRecord

        when:
        def result = service.addCategory(description)

        then:
        result.description() == description
    }

    def "deleteCategory - should delete category from workspace"() {
        given:
        def workspaceId = 1L
        def workspaceCategoryId = 10L
        def category = Stub(Category) { isDeletable() >> true }
        def workspaceCategory = Stub(WorkspaceCategory) {
            getWorkspaceId() >> workspaceId
            getCategory() >> category
        }

        workspaceContextService.getActiveWritableWorkspaceId() >> workspaceId
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

        workspaceContextService.getActiveWritableWorkspaceId() >> workspaceId
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
        def workspaceCategory = Stub(WorkspaceCategory) {
            getWorkspaceId() >> 999L
        }

        workspaceContextService.getActiveWritableWorkspaceId() >> workspaceId
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
        def category = Stub(Category) { isDeletable() >> false }
        def workspaceCategory = Stub(WorkspaceCategory) {
            getWorkspaceId() >> workspaceId
            getCategory() >> category
        }

        workspaceContextService.getActiveWritableWorkspaceId() >> workspaceId
        workspaceCategoryRepository.findById(workspaceCategoryId) >> Optional.of(workspaceCategory)

        when:
        service.deleteCategory(workspaceCategoryId)

        then:
        thrown(BusinessException)
    }

    def "updateCategory - should update all fields successfully"() {
        given:
        def categoryId = 1L
        def workspaceId = 10L
        def request = new CategoryPatchRequest("Hogar Actualizado", "HomeOutlined", "#faad14")

        def category = Mock(Category)
        def workspaceCategory = Mock(WorkspaceCategory) {
            getWorkspaceId() >> workspaceId
            getCategory() >> category
        }
        def categoryRecord = new CategoryRecord(categoryId, "Hogar Actualizado", true, true, "HomeOutlined", "#faad14")

        workspaceContextService.getActiveWritableWorkspaceId() >> workspaceId
        workspaceCategoryRepository.findById(categoryId) >> Optional.of(workspaceCategory)
        workspaceCategoryRepository.save(workspaceCategory) >> workspaceCategory
        workspaceCategoryMapper.toRecord(workspaceCategory) >> categoryRecord

        when:
        def result = service.updateCategory(categoryId, request)

        then:
        1 * category.setDescription("Hogar Actualizado")
        1 * categoryRepository.save(category)
        1 * workspaceCategory.setIconName("HomeOutlined")
        1 * workspaceCategory.setIconColor("#faad14")
        1 * eventPublisher.publishEvent(_ as CategoryUpdatedEvent)
        result.description() == "Hogar Actualizado"
        result.iconName() == "HomeOutlined"
        result.iconColor() == "#faad14"
    }

    def "updateCategory - should update only iconName when provided"() {
        given:
        def categoryId = 1L
        def workspaceId = 10L
        def request = new CategoryPatchRequest(null, "CarOutlined", null)

        def workspaceCategory = Mock(WorkspaceCategory) {
            getWorkspaceId() >> workspaceId
        }
        def categoryRecord = new CategoryRecord(categoryId, "Transporte", true, true, "CarOutlined", "#1890ff")

        workspaceContextService.getActiveWritableWorkspaceId() >> workspaceId
        workspaceCategoryRepository.findById(categoryId) >> Optional.of(workspaceCategory)
        workspaceCategoryRepository.save(workspaceCategory) >> workspaceCategory
        workspaceCategoryMapper.toRecord(workspaceCategory) >> categoryRecord

        when:
        def result = service.updateCategory(categoryId, request)

        then:
        0 * categoryRepository.save(_)
        1 * workspaceCategory.setIconName("CarOutlined")
        0 * workspaceCategory.setIconColor(_)
        1 * eventPublisher.publishEvent(_ as CategoryUpdatedEvent)
        result.iconName() == "CarOutlined"
    }

    def "updateCategory - should update only iconColor when provided"() {
        given:
        def categoryId = 1L
        def workspaceId = 10L
        def request = new CategoryPatchRequest(null, null, "#52c41a")

        def workspaceCategory = Mock(WorkspaceCategory) {
            getWorkspaceId() >> workspaceId
        }
        def categoryRecord = new CategoryRecord(categoryId, "Salud", true, true, "MedicineBoxOutlined", "#52c41a")

        workspaceContextService.getActiveWritableWorkspaceId() >> workspaceId
        workspaceCategoryRepository.findById(categoryId) >> Optional.of(workspaceCategory)
        workspaceCategoryRepository.save(workspaceCategory) >> workspaceCategory
        workspaceCategoryMapper.toRecord(workspaceCategory) >> categoryRecord

        when:
        def result = service.updateCategory(categoryId, request)

        then:
        0 * workspaceCategory.setIconName(_)
        1 * workspaceCategory.setIconColor("#52c41a")
        1 * eventPublisher.publishEvent(_ as CategoryUpdatedEvent)
        result.iconColor() == "#52c41a"
    }

    def "updateCategory - should throw EntityNotFoundException when category not found"() {
        given:
        def categoryId = 999L
        def request = new CategoryPatchRequest("Nueva Desc", null, null)

        workspaceCategoryRepository.findById(categoryId) >> Optional.empty()

        when:
        service.updateCategory(categoryId, request)

        then:
        thrown(EntityNotFoundException)
        0 * eventPublisher.publishEvent(_)
    }

    def "updateCategory - should throw PermissionDeniedException when category not in active workspace"() {
        given:
        def categoryId = 1L
        def workspaceId = 10L
        def request = new CategoryPatchRequest("Nueva Desc", null, null)

        def workspaceCategory = Mock(WorkspaceCategory) {
            getWorkspaceId() >> 999L
        }

        workspaceContextService.getActiveWritableWorkspaceId() >> workspaceId
        workspaceCategoryRepository.findById(categoryId) >> Optional.of(workspaceCategory)

        when:
        service.updateCategory(categoryId, request)

        then:
        thrown(PermissionDeniedException)
        0 * eventPublisher.publishEvent(_)
    }
}
