package api.m2.movements.unit.services

import api.m2.movements.clients.identity.IdentityClient
import api.m2.movements.enums.InvitationStatus
import api.m2.movements.exceptions.PermissionDeniedException
import api.m2.movements.clients.identity.response.WorkspaceInvitationDTO
import api.m2.movements.services.workspaces.WorkspaceQueryService
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import spock.lang.Specification

class WorkspaceQueryServiceTest extends Specification {

    IdentityClient identityClient = Mock(IdentityClient)

    WorkspaceQueryService service

    def setup() {
        service = new WorkspaceQueryService(identityClient)
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
        def expected = [new WorkspaceInvitationDTO(1L, 10L, "Hogar", "owner@test.com", InvitationStatus.PENDING, now)]
        identityClient.getInvitations() >> expected

        when:
        def result = service.getMyInvitations()

        then:
        result == expected
    }
}
