package api.m2.movements.unit.services

import api.m2.movements.clients.identity.IdentityClient
import api.m2.movements.clients.identity.requests.UserToAdd
import api.m2.movements.exceptions.BusinessException
import api.m2.movements.enums.UserSettingKey
import api.m2.movements.clients.identity.requests.AddWorkspaceRecord
import api.m2.movements.clients.identity.response.WorkspaceAdded
import api.m2.movements.clients.identity.response.UserBaseRecord
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

    // --- createWorkspace ---

    def "createWorkspace - should delegate to IdentityClient"() {
        given:
        def record = new AddWorkspaceRecord("Viajes")
        userService.getAuthenticatedUser() >> new UserBaseRecord("User", 1L)

        when:
        service.createWorkspace(record)

        then:
        1 * identityClient.createWorkspaces(1L, [record])
    }

    def "createWorkspace - should throw BusinessException when description is blank"() {
        given:
        def record = new AddWorkspaceRecord("   ")

        when:
        service.createWorkspace(record)

        then:
        thrown(BusinessException)
        0 * identityClient.createWorkspaces(_ as Long, _ as List)
    }

    def "createWorkspace - should throw BusinessException when IdentityClient rejects the request"() {
        given:
        def record = new AddWorkspaceRecord("Hogar")
        userService.getAuthenticatedUser() >> new UserBaseRecord("User", 2L)
        identityClient.createWorkspaces(2L, [record]) >> {
            throw HttpClientErrorException.create(HttpStatus.CONFLICT, "Conflict", null, null, null)
        }

        when:
        service.createWorkspace(record)

        then:
        thrown(BusinessException)
    }

    // --- leaveWorkspace ---

    def "leaveWorkspace - should throw BusinessException when IdentityClient rejects the request"() {
        given:
        userService.getAuthenticatedUser() >> new UserBaseRecord("User", 5L)
        identityClient.leaveWorkspace(99L, 5L) >> {
            throw HttpClientErrorException.create(HttpStatus.FORBIDDEN, "Forbidden", null, null, null)
        }

        when:
        service.leaveWorkspace(99L)

        then:
        thrown(BusinessException)
    }

    def "leaveWorkspace - should clear DEFAULT_WORKSPACE setting when it points to the workspace being left"() {
        given:
        userService.getAuthenticatedUser() >> new UserBaseRecord("User", 2L)
        userSettingService.getDefaultWorkspaceId(2L) >> Optional.of(10L)

        when:
        service.leaveWorkspace(10L)

        then:
        1 * identityClient.leaveWorkspace(10L, 2L)
        1 * userSettingService.deleteByKey(UserSettingKey.DEFAULT_WORKSPACE)
    }

    def "leaveWorkspace - should not touch DEFAULT_WORKSPACE setting when it points elsewhere"() {
        given:
        userService.getAuthenticatedUser() >> new UserBaseRecord("User", 2L)
        userSettingService.getDefaultWorkspaceId(2L) >> Optional.of(20L)

        when:
        service.leaveWorkspace(10L)

        then:
        1 * identityClient.leaveWorkspace(10L, 2L)
        0 * userSettingService.deleteByKey(_)
    }

    // --- createWorkspaces ---

    def "createWorkspaces - should delegate to IdentityClient"() {
        given:
        def user = UserToAdd.builder().id(1L).email("test@test.com").build()
        def workspacesToAdd = [new AddWorkspaceRecord("DEFAULT")]
        def expected = [new WorkspaceAdded(100L, "DEFAULT")]

        identityClient.createWorkspaces(1L, workspacesToAdd) >> expected

        when:
        def result = service.createWorkspaces(user, workspacesToAdd)

        then:
        result == expected
    }
}
