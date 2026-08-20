package api.m2.movements.unit.services

import api.m2.movements.clients.identity.IdentityClient
import api.m2.movements.clients.identity.response.UserMe
import api.m2.movements.exceptions.BusinessException
import api.m2.movements.enums.UserSettingKey
import api.m2.movements.clients.identity.requests.AddWorkspaceRecord
import api.m2.movements.services.settings.UserSettingService
import api.m2.movements.services.user.UserService
import api.m2.movements.services.workspaces.WorkspaceAddService
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import spock.lang.Specification

class WorkspaceAddServiceTest extends Specification {

    UserService userService = Mock(UserService)
    UserSettingService userSettingService = Mock(UserSettingService)
    IdentityClient identityClient = Mock(IdentityClient)

    WorkspaceAddService service

    def setup() {
        service = new WorkspaceAddService(
                userService,
                userSettingService,
                identityClient
        )
    }

    def userMe(Long id) {
        return new UserMe(id, "user@test.com", "User", null, "PERSONAL", new UserMe.Metadata(false, true, []))
    }

    // --- createWorkspace ---

    def "createWorkspace - should delegate to IdentityClient"() {
        given:
        def record = new AddWorkspaceRecord("Viajes")

        when:
        service.createWorkspace(record)

        then:
        1 * identityClient.createWorkspaces([record])
    }

    def "createWorkspace - should throw BusinessException when description is blank"() {
        given:
        def record = new AddWorkspaceRecord("   ")

        when:
        service.createWorkspace(record)

        then:
        thrown(BusinessException)
        0 * identityClient.createWorkspaces(_ as List)
    }

    def "createWorkspace - should propagate IdentityClient failure"() {
        given:
        def record = new AddWorkspaceRecord("Hogar")
        identityClient.createWorkspaces([record]) >> {
            throw HttpClientErrorException.create(HttpStatus.CONFLICT, "Conflict", null, null, null)
        }

        when:
        service.createWorkspace(record)

        then:
        thrown(HttpClientErrorException)
    }

    // --- leaveWorkspace ---

    def "leaveWorkspace - should propagate IdentityClient failure"() {
        given:
        userService.getMe() >> userMe(5L)
        identityClient.leaveWorkspace(99L) >> {
            throw HttpClientErrorException.create(HttpStatus.FORBIDDEN, "Forbidden", null, null, null)
        }

        when:
        service.leaveWorkspace(99L)

        then:
        thrown(HttpClientErrorException)
    }

    def "leaveWorkspace - should clear DEFAULT_WORKSPACE setting when it points to the workspace being left"() {
        given:
        userService.getMe() >> userMe(2L)
        userSettingService.getDefaultWorkspaceId(2L) >> Optional.of(10L)

        when:
        service.leaveWorkspace(10L)

        then:
        1 * identityClient.leaveWorkspace(10L)
        1 * userSettingService.deleteByKey(UserSettingKey.DEFAULT_WORKSPACE)
    }

    def "leaveWorkspace - should not touch DEFAULT_WORKSPACE setting when it points elsewhere"() {
        given:
        userService.getMe() >> userMe(2L)
        userSettingService.getDefaultWorkspaceId(2L) >> Optional.of(20L)

        when:
        service.leaveWorkspace(10L)

        then:
        1 * identityClient.leaveWorkspace(10L)
        0 * userSettingService.deleteByKey(_)
    }

    // --- removeMember ---

    def "removeMember - should delegate to IdentityClient"() {
        given:
        userSettingService.getDefaultWorkspaceId(7L) >> Optional.empty()

        when:
        service.removeMember(10L, 7L)

        then:
        1 * identityClient.removeMember(10L, 7L)
    }

    def "removeMember - should clear the removed user's DEFAULT_WORKSPACE setting when it points to that workspace"() {
        given:
        userSettingService.getDefaultWorkspaceId(7L) >> Optional.of(10L)

        when:
        service.removeMember(10L, 7L)

        then:
        1 * userSettingService.deleteByKeyForUser(7L, UserSettingKey.DEFAULT_WORKSPACE)
    }

    def "removeMember - should not touch DEFAULT_WORKSPACE setting when it points elsewhere"() {
        given:
        userSettingService.getDefaultWorkspaceId(7L) >> Optional.of(20L)

        when:
        service.removeMember(10L, 7L)

        then:
        0 * userSettingService.deleteByKeyForUser(_, _)
    }

    def "removeMember - should propagate IdentityClient failure (e.g. actor not authorized)"() {
        given:
        identityClient.removeMember(10L, 7L) >> {
            throw HttpClientErrorException.create(HttpStatus.FORBIDDEN, "Forbidden", null, null, null)
        }

        when:
        service.removeMember(10L, 7L)

        then:
        thrown(HttpClientErrorException)
    }

}
