package api.m2.movements.unit.services

import api.m2.movements.entities.Category
import api.m2.movements.entities.Movement
import api.m2.movements.entities.WorkspaceCategory
import api.m2.movements.mappers.CategoryMapper
import api.m2.movements.mappers.MovementMapper
import api.m2.movements.records.categories.CategoryRecord
import api.m2.movements.records.movements.MovementRecord
import api.m2.movements.records.movements.MovementSearchFilterRecord
import api.m2.movements.repositories.MovementRepository
import api.m2.movements.repositories.WorkspaceCategoryRepository
import api.m2.movements.services.movements.MovementGetService
import api.m2.movements.services.workspaces.WorkspaceContextService
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import spock.lang.Specification

class MovementGetServiceTest extends Specification {

    MovementRepository movementRepository = Mock(MovementRepository)
    MovementMapper movementMapper = Mock(MovementMapper)
    CategoryMapper categoryMapper = Mock(CategoryMapper)
    WorkspaceContextService workspaceContextService = Mock(WorkspaceContextService)
    WorkspaceCategoryRepository workspaceCategoryRepository = Mock(WorkspaceCategoryRepository)

    MovementGetService service

    def setup() {
        service = new MovementGetService(
                movementRepository,
                movementMapper,
                categoryMapper,
                workspaceContextService,
                workspaceCategoryRepository
        )
    }

    def "getExpensesBy - should enrich movements with icons from workspace categories"() {
        given:
        def filter = new MovementSearchFilterRecord(null, null, null, null, null, null, null, null)
        def pageable = PageRequest.of(0, 10)

        def category1 = Stub(Category) { getId() >> 1L }
        def category2 = Stub(Category) { getId() >> 2L }
        
        def movement1 = Stub(Movement) { getCategory() >> category1 }
        def movement2 = Stub(Movement) { getCategory() >> category2 }
        
        def workspaceCategory1 = Stub(WorkspaceCategory) {
            getCategory() >> category1
            getIconName() >> "HomeOutlined"
            getIconColor() >> "#faad14"
        }
        def workspaceCategory2 = Stub(WorkspaceCategory) {
            getCategory() >> category2
            getIconName() >> "CarOutlined"
            getIconColor() >> "#1890ff"
        }
        
        def baseRecord1 = Stub(MovementRecord) {
            id() >> 1L
            category() >> Stub(CategoryRecord)
        }
        def baseRecord2 = Stub(MovementRecord) {
            id() >> 2L
            category() >> Stub(CategoryRecord)
        }
        
        def enrichedCategory1 = Stub(CategoryRecord) {
            iconName() >> "HomeOutlined"
            iconColor() >> "#faad14"
        }
        def enrichedCategory2 = Stub(CategoryRecord) {
            iconName() >> "CarOutlined"
            iconColor() >> "#1890ff"
        }

        workspaceContextService.getActiveWorkspaceId() >> 1L
        movementRepository.getExpenseBy([1L], filter, pageable) >> new PageImpl([movement1, movement2])
        workspaceCategoryRepository.findByWorkspaceIdAndIsActiveTrue(1L) >> [workspaceCategory1, workspaceCategory2]
        
        movementMapper.toRecord(movement1) >> baseRecord1
        movementMapper.toRecord(movement2) >> baseRecord2
        
        categoryMapper.toRecordWithIcons(category1, workspaceCategory1) >> enrichedCategory1
        categoryMapper.toRecordWithIcons(category2, workspaceCategory2) >> enrichedCategory2

        when:
        def result = service.getExpensesBy(filter, pageable)

        then:
        result.content.size() == 2
        // Verificar que se llamó a enriquecer las categorías
        1 * categoryMapper.toRecordWithIcons(category1, workspaceCategory1)
        1 * categoryMapper.toRecordWithIcons(category2, workspaceCategory2)
    }

    def "getExpensesBy - should return empty page when user has no workspaces"() {
        given:
        def filter = new MovementSearchFilterRecord(null, null, null, null, null, null, null, null)
        def pageable = PageRequest.of(0, 10)

        workspaceContextService.getActiveWorkspaceId() >> 1L
        movementRepository.getExpenseBy([1L], filter, pageable) >> new PageImpl([])
        workspaceCategoryRepository.findByWorkspaceIdAndIsActiveTrue(1L) >> []

        when:
        def result = service.getExpensesBy(filter, pageable)

        then:
        result.content.isEmpty()
    }
    
    def "getExpensesBy - should handle movements without category"() {
        given:
        def filter = new MovementSearchFilterRecord(null, null, null, null, null, null, null, null)
        def pageable = PageRequest.of(0, 10)

        def movement = Stub(Movement) { getCategory() >> null }
        def baseRecord = Stub(MovementRecord) {
            id() >> 1L
            category() >> null
        }

        workspaceContextService.getActiveWorkspaceId() >> 1L
        movementRepository.getExpenseBy([1L], filter, pageable) >> new PageImpl([movement])
        workspaceCategoryRepository.findByWorkspaceIdAndIsActiveTrue(1L) >> []
        movementMapper.toRecord(movement) >> baseRecord

        when:
        def result = service.getExpensesBy(filter, pageable)

        then:
        result.content.size() == 1
        // No debería intentar enriquecer categorías nulas
        0 * categoryMapper.toRecordWithIcons(_, _)
    }
    
    def "getExpensesBy - should use default icons when category not in workspace"() {
        given:
        def filter = new MovementSearchFilterRecord(null, null, null, null, null, null, null, null)
        def pageable = PageRequest.of(0, 10)

        def category = Stub(Category) { getId() >> 99L } // Categoría que NO está en workspace
        def movement = Stub(Movement) { getCategory() >> category }
        
        def baseRecord = Stub(MovementRecord) {
            id() >> 1L
            category() >> Stub(CategoryRecord)
        }
        
        def enrichedCategoryWithDefaults = Stub(CategoryRecord) {
            iconName() >> "QuestionOutlined"
            iconColor() >> "#d9d9d9"
        }

        workspaceContextService.getActiveWorkspaceId() >> 1L
        movementRepository.getExpenseBy([1L], filter, pageable) >> new PageImpl([movement])
        workspaceCategoryRepository.findByWorkspaceIdAndIsActiveTrue(1L) >> [] // Workspace sin categorías
        movementMapper.toRecord(movement) >> baseRecord
        categoryMapper.toRecordWithIcons(category, null) >> enrichedCategoryWithDefaults

        when:
        def result = service.getExpensesBy(filter, pageable)

        then:
        result.content.size() == 1
        // Debería llamar al mapper con workspaceCategory = null para usar defaults
        1 * categoryMapper.toRecordWithIcons(category, null)
    }
}
