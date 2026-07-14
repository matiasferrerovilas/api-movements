package api.m2.movements.unit.services


import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.identity.records.users.UserBaseRecord
import api.m2.movements.movements.services.settings.UserSettingService
import api.m2.movements.identity.services.user.UserService
import api.m2.movements.identity.services.workspaces.WorkspaceContextService
import spock.lang.Specification

class WorkspaceContextServiceTest extends Specification {

    UserSettingService userSettingService = Mock()
    UserService userService = Mock()
    WorkspaceContextService service

    def setup() {
        service = new WorkspaceContextService(userSettingService, userService)
    }

    def "getActiveWorkspaceId - should return workspace id when user has default configured"() {
        given:
        userService.getAuthenticatedUser() >> new UserBaseRecord("User", 1L)
        userSettingService.getDefaultWorkspaceId(1L) >> Optional.of(10L)

        when:
        def result = service.getActiveWorkspaceId()

        then:
        result == 10L
    }

    def "getActiveWorkspaceId - should throw EntityNotFoundException when user has no default workspace"() {
        given:
        userService.getAuthenticatedUser() >> new UserBaseRecord("User", 1L)
        userSettingService.getDefaultWorkspaceId(1L) >> Optional.empty()

        when:
        service.getActiveWorkspaceId()

        then:
        def ex = thrown(EntityNotFoundException)
        ex.message == "Usuario sin workspace por defecto configurado"
    }
}
