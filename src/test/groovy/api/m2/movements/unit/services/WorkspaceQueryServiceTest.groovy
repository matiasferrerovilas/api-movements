package api.m2.movements.unit.services

import api.m2.movements.clients.identity.IdentityClient
import api.m2.movements.enums.InvitationStatus
import api.m2.movements.exceptions.BusinessException
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.exceptions.PermissionDeniedException
import api.m2.movements.records.users.UserBaseRecord
import api.m2.movements.records.workspaces.WorkspaceInvitationDTO
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

    def "getMyInvitations - should delegate to IdentityClient"() {
        given:
        def now = java.time.LocalDateTime.now()
        def expected = [new WorkspaceInvitationDTO(1L, 10L, "Hogar", "owner@test.com", InvitationStatus.PENDING, now)]
        identityClient.getInvitations() >> expected

        when:
        def result = service.getMyInvitations()

        then:
        result == expected
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
