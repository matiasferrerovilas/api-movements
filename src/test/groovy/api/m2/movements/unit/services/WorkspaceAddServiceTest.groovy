package api.m2.movements.unit.services

import api.m2.movements.clients.identity.IdentityGateway
import api.m2.movements.clients.identity.response.UserMe
import api.m2.movements.exceptions.BusinessException
import api.m2.movements.enums.UserSettingKey
import api.m2.movements.clients.identity.requests.AddWorkspaceRecord
import api.m2.movements.services.settings.UserSettingService
import api.m2.movements.services.user.UserService
import api.m2.movements.services.workspaces.IdentityCacheInvalidator
import api.m2.movements.services.workspaces.WorkspaceAddService
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import spock.lang.Specification

class WorkspaceAddServiceTest extends Specification {

    UserService userService = Mock(UserService)
    UserSettingService userSettingService = Mock(UserSettingService)
    IdentityGateway identityGateway = Mock(IdentityGateway)
    IdentityCacheInvalidator identityCacheInvalidator = Mock(IdentityCacheInvalidator)

    WorkspaceAddService service

    def setup() {
        service = new WorkspaceAddService(
                userService,
                userSettingService,
                identityGateway,
                identityCacheInvalidator
        )
    }

    def userMe(Long id) {
        return new UserMe(id, "user@test.com", "User", null, "PERSONAL", new UserMe.Metadata(false, true, [], null))
    }

    def "createWorkspace - should delegate to IdentityGateway"() {
        given:
        def record = new AddWorkspaceRecord("Viajes")

        when:
        service.createWorkspace(record)

        then:
        1 * identityGateway.createWorkspaces([record])
    }

    def "createWorkspace - should throw BusinessException when description is blank"() {
        given:
        def record = new AddWorkspaceRecord("   ")

        when:
        service.createWorkspace(record)

        then:
        thrown(BusinessException)
        0 * identityGateway.createWorkspaces(_ as List)
    }

    def "createWorkspace - should propagate IdentityGateway failure"() {
        given:
        def record = new AddWorkspaceRecord("Hogar")
        identityGateway.createWorkspaces([record]) >> {
            throw HttpClientErrorException.create(HttpStatus.CONFLICT, "Conflict", null, null, null)
        }

        when:
        service.createWorkspace(record)

        then:
        thrown(HttpClientErrorException)
    }

    def "leaveWorkspace - should propagate IdentityGateway failure"() {
        given:
        userService.getMe() >> userMe(5L)
        identityGateway.leaveWorkspace(99L) >> {
            throw HttpClientErrorException.create(HttpStatus.FORBIDDEN, "Forbidden", null, null, null)
        }

        when:
        service.leaveWorkspace(99L)

        then:
        thrown(HttpClientErrorException)
        0 * identityCacheInvalidator.invalidateAuthorization()
    }

    def "leaveWorkspace - should invalidate the cached authorization once api-identity confirmed it"() {
        given:
        userService.getMe() >> userMe(5L)
        userSettingService.getDefaultWorkspaceId(5L) >> Optional.empty()

        when:
        service.leaveWorkspace(99L)

        then:
        1 * identityGateway.leaveWorkspace(99L)
        1 * identityCacheInvalidator.invalidateAuthorization()
    }

    def "removeMember - should invalidate the cached authorization once api-identity confirmed it"() {
        given:
        userSettingService.getDefaultWorkspaceId(7L) >> Optional.empty()

        when:
        service.removeMember(10L, 7L)

        then:
        1 * identityGateway.removeMember(10L, 7L)
        1 * identityCacheInvalidator.invalidateAuthorization()
    }

    def "leaveWorkspace - should clear DEFAULT_WORKSPACE setting when it points to the workspace being left"() {
        given:
        userService.getMe() >> userMe(2L)
        userSettingService.getDefaultWorkspaceId(2L) >> Optional.of(10L)

        when:
        service.leaveWorkspace(10L)

        then:
        1 * identityGateway.leaveWorkspace(10L)
        1 * userSettingService.deleteByKey(UserSettingKey.DEFAULT_WORKSPACE)
    }

    def "leaveWorkspace - should not touch DEFAULT_WORKSPACE setting when it points elsewhere"() {
        given:
        userService.getMe() >> userMe(2L)
        userSettingService.getDefaultWorkspaceId(2L) >> Optional.of(20L)

        when:
        service.leaveWorkspace(10L)

        then:
        1 * identityGateway.leaveWorkspace(10L)
        0 * userSettingService.deleteByKey(_)
    }

    def "removeMember - should delegate to IdentityGateway"() {
        given:
        userSettingService.getDefaultWorkspaceId(7L) >> Optional.empty()

        when:
        service.removeMember(10L, 7L)

        then:
        1 * identityGateway.removeMember(10L, 7L)
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

    def "removeMember - should propagate IdentityGateway failure (e.g. actor not authorized)"() {
        given:
        identityGateway.removeMember(10L, 7L) >> {
            throw HttpClientErrorException.create(HttpStatus.FORBIDDEN, "Forbidden", null, null, null)
        }

        when:
        service.removeMember(10L, 7L)

        then:
        thrown(HttpClientErrorException)
        0 * identityCacheInvalidator.invalidateAuthorization()
    }

}
