package api.m2.movements.unit.services

import api.m2.movements.entities.Movement
import api.m2.movements.mappers.MovementMapper
import api.m2.movements.records.movements.MovementRecord
import api.m2.movements.records.movements.MovementSearchFilterRecord
import api.m2.movements.records.users.UserBaseRecord
import api.m2.movements.records.workspaces.WorkspaceRecord
import api.m2.movements.repositories.MovementRepository
import api.m2.movements.services.movements.MovementGetService
import api.m2.movements.services.workspaces.WorkspaceQueryService
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import spock.lang.Specification

class MovementGetServiceTest extends Specification {

    MovementRepository movementRepository = Mock(MovementRepository)
    MovementMapper movementMapper = Mock(MovementMapper)
    WorkspaceQueryService workspaceQueryService = Mock(WorkspaceQueryService)

    MovementGetService service

    def setup() {
        service = new MovementGetService(
                movementRepository,
                movementMapper,
                workspaceQueryService
        )
    }

    def "getExpensesBy - should return paged movements for user workspaces"() {
        given:
        def owner = new UserBaseRecord("owner@test.com", 1L)
        def workspace1 = new WorkspaceRecord(1L, "Workspace 1", owner, [])
        def workspace2 = new WorkspaceRecord(2L, "Workspace 2", owner, [])
        def filter = new MovementSearchFilterRecord(null, null, null, null, null, null, null, null)
        def pageable = PageRequest.of(0, 10)

        def movement1 = Stub(Movement)
        def movement2 = Stub(Movement)
        def movementRecord1 = Stub(MovementRecord)
        def movementRecord2 = Stub(MovementRecord)

        workspaceQueryService.findAllWorkspacesOfLogInUser() >> [workspace1, workspace2]
        movementRepository.getExpenseBy([1L, 2L], filter, pageable) >> new PageImpl([movement1, movement2])
        movementMapper.toRecord(movement1) >> movementRecord1
        movementMapper.toRecord(movement2) >> movementRecord2

        when:
        def result = service.getExpensesBy(filter, pageable)

        then:
        result.content.size() == 2
        result.content[0] == movementRecord1
        result.content[1] == movementRecord2
    }

    def "getExpensesBy - should return empty page when user has no workspaces"() {
        given:
        def filter = new MovementSearchFilterRecord(null, null, null, null, null, null, null, null)
        def pageable = PageRequest.of(0, 10)

        workspaceQueryService.findAllWorkspacesOfLogInUser() >> []
        movementRepository.getExpenseBy([], filter, pageable) >> new PageImpl([])

        when:
        def result = service.getExpensesBy(filter, pageable)

        then:
        result.content.isEmpty()
    }
}
