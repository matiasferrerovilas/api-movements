package api.m2.movements.unit.services

import api.m2.movements.clients.identity.IdentityClient
import api.m2.movements.clients.identity.response.UserMe
import api.m2.movements.enums.InvitationStatus
import api.m2.movements.enums.WorkspaceRole
import api.m2.movements.clients.identity.response.WorkspaceMemberDTO
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.exceptions.PermissionDeniedException
import api.m2.movements.exceptions.ServiceException
import api.m2.movements.clients.identity.response.WorkspaceInvitationDTO
import api.m2.movements.clients.identity.response.WorkspaceSentInvitationDTO
import api.m2.movements.services.user.UserService
import api.m2.movements.services.workspaces.WorkspaceQueryService
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException
import spock.lang.Specification

class WorkspaceQueryServiceTest extends Specification {

    IdentityClient identityClient = Mock(IdentityClient)
    UserService userService = Mock(UserService)

    WorkspaceQueryService service

    def setup() {
        service = new WorkspaceQueryService(identityClient, userService)
    }

    def userMe(WorkspaceRole role) {
        return new UserMe(1L, "a@test.com", "A", "A", "PERSONAL",
                new UserMe.Metadata(false, true, [], role))
    }

    def "findWorkspaceNameById - should return the name of the workspace"() {
        given:
        identityClient.getWorkspaces() >> [
                new WorkspaceMemberDTO(1L, 20L, "Otro", new WorkspaceMemberDTO.Metadata([], WorkspaceRole.OWNER, null, false)),
                new WorkspaceMemberDTO(2L, 10L, "Familia", new WorkspaceMemberDTO.Metadata([], WorkspaceRole.OWNER, null, true))
        ]

        expect:
        service.findWorkspaceNameById(10L) == "Familia"
    }

    def "findWorkspaceNameById - should return null instead of failing when api-identity is unreachable"() {
        given:
        identityClient.getWorkspaces() >> { throw new ResourceAccessException("read timed out") }

        expect:
        service.findWorkspaceNameById(10L) == null
    }

    def "findWorkspaceNameById - should return null instead of failing on a server error from api-identity"() {
        given:
        identityClient.getWorkspaces() >> { throw new ServiceException("api-identity caido") }

        expect:
        service.findWorkspaceNameById(10L) == null
    }

    def "findWorkspaceNameById - should still fail when the workspace is not one of the caller's"() {
        given:
        identityClient.getWorkspaces() >> []

        when:
        service.findWorkspaceNameById(10L)

        then:
        thrown(EntityNotFoundException)
    }

    def "verifyCanWrite - should not throw when caller is at least COLLABORATOR"() {
        given:
        userService.getMe(1L) >> userMe(WorkspaceRole.COLLABORATOR)

        when:
        service.verifyCanWrite(1L)

        then:
        noExceptionThrown()
    }

    def "verifyCanWrite - should throw PermissionDeniedException when caller is READ_ONLY"() {
        given:
        userService.getMe(1L) >> userMe(WorkspaceRole.READ_ONLY)

        when:
        service.verifyCanWrite(1L)

        then:
        thrown(PermissionDeniedException)
    }

    def "verifyCanWrite - should throw PermissionDeniedException when caller is not a member"() {
        given:
        userService.getMe(1L) >> userMe(null)

        when:
        service.verifyCanWrite(1L)

        then:
        thrown(PermissionDeniedException)
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

    def "getMyInvitations - should delegate to IdentityClient"() {
        given:
        def now = java.time.LocalDateTime.now()
        def expected = [new WorkspaceInvitationDTO(1L, 10L, "Hogar", "owner@test.com", InvitationStatus.PENDING, WorkspaceRole.COLLABORATOR, now)]
        identityClient.getInvitations() >> expected

        when:
        def result = service.getMyInvitations()

        then:
        result == expected
    }

    def "getSentInvitations - should delegate to IdentityClient"() {
        given:
        def now = java.time.LocalDateTime.now()
        def expected = [new WorkspaceSentInvitationDTO(1L, 10L, "Hogar", "invited@test.com", InvitationStatus.PENDING, WorkspaceRole.COLLABORATOR, now)]
        identityClient.getSentInvitations() >> expected

        when:
        def result = service.getSentInvitations()

        then:
        result == expected
    }

    def "cancelInvitation - should delegate to IdentityClient"() {
        when:
        service.cancelInvitation(5L)

        then:
        1 * identityClient.cancelInvitation(5L)
    }
}
