package api.m2.movements.unit.services


import api.m2.movements.clients.identity.response.UserMe
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.services.settings.UserSettingService
import api.m2.movements.exceptions.PermissionDeniedException
import api.m2.movements.services.user.UserService
import api.m2.movements.services.workspaces.WorkspaceContextService
import api.m2.movements.services.workspaces.WorkspaceQueryService
import spock.lang.Specification

class WorkspaceContextServiceTest extends Specification {

    UserSettingService userSettingService = Mock()
    UserService userService = Mock()
    WorkspaceQueryService workspaceQueryService = Mock()
    WorkspaceContextService service

    def setup() {
        service = new WorkspaceContextService(userSettingService, userService, workspaceQueryService)
    }

    def "getActiveWorkspaceId - should return workspace id when user has default configured"() {
        given:
        userService.getMe() >> buildUserMe()
        userSettingService.getDefaultWorkspaceId(1L) >> Optional.of(10L)

        when:
        def result = service.getActiveWorkspaceId()

        then:
        result == 10L
    }

    def "getActiveWorkspaceId - should throw EntityNotFoundException when user has no default workspace"() {
        given:
        userService.getMe() >> buildUserMe()
        userSettingService.getDefaultWorkspaceId(1L) >> Optional.empty()

        when:
        service.getActiveWorkspaceId()

        then:
        def ex = thrown(EntityNotFoundException)
        ex.message == "Usuario sin workspace por defecto configurado"
    }

    def "getActiveWritableWorkspaceId - should return the active workspace id after verifying the caller can write"() {
        given:
        userService.getMe() >> buildUserMe()
        userSettingService.getDefaultWorkspaceId(1L) >> Optional.of(10L)

        when:
        def result = service.getActiveWritableWorkspaceId()

        then:
        1 * workspaceQueryService.verifyCanWrite(10L)
        result == 10L
    }

    def "getActiveWritableWorkspaceId - should propagate PermissionDeniedException when the caller cannot write"() {
        given:
        userService.getMe() >> buildUserMe()
        userSettingService.getDefaultWorkspaceId(1L) >> Optional.of(10L)
        workspaceQueryService.verifyCanWrite(10L) >> {
            throw new PermissionDeniedException("Los miembros de solo lectura no pueden crear ni modificar recursos")
        }

        when:
        service.getActiveWritableWorkspaceId()

        then:
        thrown(PermissionDeniedException)
    }

    def "getActiveWritableWorkspaceId - should not check permissions when there is no active workspace"() {
        given:
        userService.getMe() >> buildUserMe()
        userSettingService.getDefaultWorkspaceId(1L) >> Optional.empty()

        when:
        service.getActiveWritableWorkspaceId()

        then:
        thrown(EntityNotFoundException)
        0 * workspaceQueryService.verifyCanWrite(_ as Long)
    }

    private static UserMe buildUserMe() {
        return new UserMe(1L, "a@a.com", "User", "Test", "ADMIN",
                new UserMe.Metadata(false, true, [], null))
    }
}
