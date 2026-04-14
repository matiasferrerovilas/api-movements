package api.m2.movements.unit.services

import api.m2.movements.entities.User
import api.m2.movements.entities.Workspace
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.repositories.WorkspaceRepository
import api.m2.movements.services.settings.UserSettingService
import api.m2.movements.services.user.UserService
import api.m2.movements.services.workspaces.WorkspaceContextService
import spock.lang.Specification

class WorkspaceContextServiceTest extends Specification {

    UserSettingService userSettingService = Mock()
    WorkspaceRepository workspaceRepository = Mock()
    UserService userService = Mock()
    WorkspaceContextService service

    def setup() {
        service = new WorkspaceContextService(userSettingService, workspaceRepository, userService)
    }

    def "getActiveWorkspace - should return workspace when user has default configured"() {
        given:
        def user = Stub(User) { getId() >> 1L }
        def workspace = Stub(Workspace) { getId() >> 10L }

        userService.getAuthenticatedUser() >> user
        userSettingService.getDefaultWorkspaceId(user) >> Optional.of(10L)
        workspaceRepository.findById(10L) >> Optional.of(workspace)

        when:
        def result = service.getActiveWorkspace()

        then:
        result == workspace
    }

    def "getActiveWorkspace - should throw EntityNotFoundException when user has no default workspace"() {
        given:
        def user = Stub(User) { getId() >> 1L }

        userService.getAuthenticatedUser() >> user
        userSettingService.getDefaultWorkspaceId(user) >> Optional.empty()

        when:
        service.getActiveWorkspace()

        then:
        def ex = thrown(EntityNotFoundException)
        ex.message == "Usuario sin workspace por defecto configurado"
    }

    def "getActiveWorkspace - should throw EntityNotFoundException when workspace does not exist"() {
        given:
        def user = Stub(User) { getId() >> 1L }

        userService.getAuthenticatedUser() >> user
        userSettingService.getDefaultWorkspaceId(user) >> Optional.of(999L)
        workspaceRepository.findById(999L) >> Optional.empty()

        when:
        service.getActiveWorkspace()

        then:
        def ex = thrown(EntityNotFoundException)
        ex.message == "Workspace no encontrado: 999"
    }

    def "getActiveWorkspaceId - should return workspace id"() {
        given:
        def user = Stub(User) { getId() >> 1L }
        def workspace = Stub(Workspace) { getId() >> 10L }

        userService.getAuthenticatedUser() >> user
        userSettingService.getDefaultWorkspaceId(user) >> Optional.of(10L)
        workspaceRepository.findById(10L) >> Optional.of(workspace)

        when:
        def result = service.getActiveWorkspaceId()

        then:
        result == 10L
    }
}
