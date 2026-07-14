package api.m2.movements.unit.services

import api.m2.movements.clients.identity.IdentityClient
import api.m2.movements.exceptions.BusinessException
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.exceptions.PermissionDeniedException
import api.m2.movements.records.users.UserBaseRecord
import api.m2.movements.records.workspaces.WorkspacesWithUser
import api.m2.movements.services.settings.UserSettingService
import api.m2.movements.services.user.UserService
import api.m2.movements.services.workspaces.WorkspaceQueryService
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import spock.lang.Specification

class WorkspaceQueryServiceTest extends Specification {

    IdentityClient identityClient = Mock(IdentityClient)
    UserService userService = Mock(UserService)
    UserSettingService userSettingService = Mock(UserSettingService)

    WorkspaceQueryService service

    def setup() {
        service = new WorkspaceQueryService(
                identityClient,
                userService,
                userSettingService
        )
    }

    def "verifyUserIsMemberOfWorkspace - should not throw when IdentityClient confirms membership"() {
        given:
        identityClient.verifyMembership(1L, 42L) >> {}

        when:
        service.verifyUserIsMemberOfWorkspace(1L, 42L)

        then:
        noExceptionThrown()
    }

    def "verifyUserIsMemberOfWorkspace - should throw PermissionDeniedException when IdentityClient rejects membership"() {
        given:
        identityClient.verifyMembership(1L, 99L) >> {
            throw HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null)
        }

        when:
        service.verifyUserIsMemberOfWorkspace(1L, 99L)

        then:
        thrown(PermissionDeniedException)
    }

    def "getAllWorkspaceDetails - should mark workspace as default when it matches DEFAULT_WORKSPACE setting"() {
        given:
        userService.getAuthenticatedUser() >> new UserBaseRecord("User", 1L)
        userSettingService.getDefaultWorkspaceId(1L) >> Optional.of(10L)
        identityClient.getWorkspaces(1L) >> [
                new WorkspacesWithUser(10L, "Hogar", 2L, "user@test.com"),
                new WorkspacesWithUser(20L, "Viajes", 1L, "user@test.com"),
        ]

        when:
        def result = service.getAllWorkspaceDetails()

        then:
        result.size() == 2
        result[0].id() == 10L
        result[0].name() == "Hogar"
        result[0].membersCount() == 2
        result[0].isDefault() == true
        result[1].id() == 20L
        result[1].isDefault() == false
    }

    def "getAllWorkspaceDetails - should mark isDefault false when no DEFAULT_WORKSPACE setting exists"() {
        given:
        userService.getAuthenticatedUser() >> new UserBaseRecord("User", 1L)
        userSettingService.getDefaultWorkspaceId(1L) >> Optional.empty()
        identityClient.getWorkspaces(1L) >> [new WorkspacesWithUser(10L, "Hogar", 1L, "user@test.com")]

        when:
        def result = service.getAllWorkspaceDetails()

        then:
        result.size() == 1
        result[0].isDefault() == false
    }

    def "findWorkspaceIdByName - should return matching workspace id"() {
        given:
        userService.getAuthenticatedUser() >> new UserBaseRecord("User", 1L)
        identityClient.getWorkspaces(1L) >> [
                new WorkspacesWithUser(10L, "Hogar", 1L, "user@test.com"),
                new WorkspacesWithUser(20L, "Viajes", 1L, "user@test.com"),
        ]

        when:
        def result = service.findWorkspaceIdByName("Viajes")

        then:
        result == 20L
    }

    def "findWorkspaceIdByName - should throw BusinessException when name is blank"() {
        when:
        service.findWorkspaceIdByName("   ")

        then:
        thrown(BusinessException)
    }

    def "findWorkspaceIdByName - should throw EntityNotFoundException when no workspace matches"() {
        given:
        userService.getAuthenticatedUser() >> new UserBaseRecord("User", 1L)
        identityClient.getWorkspaces(1L) >> [new WorkspacesWithUser(10L, "Hogar", 1L, "user@test.com")]

        when:
        service.findWorkspaceIdByName("Inexistente")

        then:
        thrown(EntityNotFoundException)
    }
}
