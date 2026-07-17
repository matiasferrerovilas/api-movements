package api.m2.movements.unit.services


import api.m2.movements.clients.identity.IdentityClient
import api.m2.movements.clients.identity.response.UserMe
import api.m2.movements.enums.WorkspaceRole
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.clients.identity.response.WorkspaceMemberDTO
import api.m2.movements.services.settings.UserSettingService
import api.m2.movements.services.user.UserService
import api.m2.movements.services.workspaces.WorkspaceContextService
import spock.lang.Specification

class WorkspaceContextServiceTest extends Specification {

    UserSettingService userSettingService = Mock()
    UserService userService = Mock()
    IdentityClient identityClient = Mock()
    WorkspaceContextService service

    def setup() {
        service = new WorkspaceContextService(userSettingService, userService, identityClient)
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

    def "getActiveWorkspace - should return the workspace matching the active workspace id"() {
        given:
        userService.getMe() >> buildUserMe()
        userSettingService.getDefaultWorkspaceId(1L) >> Optional.of(10L)
        identityClient.getWorkspaces() >> [
                new WorkspaceMemberDTO(1L, 20L, "Otro",
                        new WorkspaceMemberDTO.Metadata([], WorkspaceRole.OWNER, null, false)),
                new WorkspaceMemberDTO(2L, 10L, "Familia",
                        new WorkspaceMemberDTO.Metadata([], WorkspaceRole.OWNER, null, true))
        ]

        when:
        def result = service.getActiveWorkspace()

        then:
        result.workspaceId() == 10L
        result.workspaceName() == "Familia"
    }

    def "getActiveWorkspace - should throw EntityNotFoundException when active workspace is not in identity response"() {
        given:
        userService.getMe() >> buildUserMe()
        userSettingService.getDefaultWorkspaceId(1L) >> Optional.of(10L)
        identityClient.getWorkspaces() >> []

        when:
        service.getActiveWorkspace()

        then:
        thrown(EntityNotFoundException)
    }

    private static UserMe buildUserMe() {
        return new UserMe(1L, "a@a.com", "User", "Test", "ADMIN",
                new UserMe.Metadata(false, true, []))
    }
}
