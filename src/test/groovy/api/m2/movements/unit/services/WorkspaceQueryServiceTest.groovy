package api.m2.movements.unit.services

import api.m2.movements.entities.User
import api.m2.movements.entities.WorkspaceMember
import api.m2.movements.exceptions.PermissionDeniedException
import api.m2.movements.mappers.WorkspaceMapper
import api.m2.movements.mappers.WorkspaceMapperImpl
import api.m2.movements.mappers.UserMapper
import api.m2.movements.projections.WorkspaceSummaryProjection
import api.m2.movements.records.workspaces.WorkspaceDetail
import api.m2.movements.repositories.MembershipRepository
import api.m2.movements.repositories.WorkspaceRepository
import api.m2.movements.services.settings.UserSettingService
import api.m2.movements.services.user.UserService
import api.m2.movements.services.workspaces.WorkspaceQueryService
import org.mapstruct.factory.Mappers
import org.springframework.test.util.ReflectionTestUtils
import spock.lang.Specification

class WorkspaceQueryServiceTest extends Specification {

    WorkspaceRepository workspaceRepository = Mock(WorkspaceRepository)
    UserService userService = Mock(UserService)
    WorkspaceMapper workspaceMapper
    MembershipRepository membershipRepository = Mock(MembershipRepository)
    UserSettingService userSettingService = Mock(UserSettingService)

    WorkspaceQueryService service

    def setup() {
        UserMapper userMapper = Mappers.getMapper(UserMapper)
        workspaceMapper = new WorkspaceMapperImpl()
        ReflectionTestUtils.setField(workspaceMapper, "userMapper", userMapper)

        service = new WorkspaceQueryService(
                workspaceRepository,
                userService,
                workspaceMapper,
                membershipRepository,
                userSettingService
        )
    }

    def "verifyUserIsMemberOfWorkspace - should not throw when user is member of workspace"() {
        given:
        membershipRepository.findMember(1L, 42L) >> Optional.of(Stub(WorkspaceMember))

        when:
        service.verifyUserIsMemberOfWorkspace(1L, 42L)

        then:
        noExceptionThrown()
    }

    def "verifyUserIsMemberOfWorkspace - should throw PermissionDeniedException when user is not member"() {
        given:
        membershipRepository.findMember(1L, 99L) >> Optional.empty()

        when:
        service.verifyUserIsMemberOfWorkspace(1L, 99L)

        then:
        thrown(PermissionDeniedException)
    }

    def "verifyUserIsMemberOfWorkspace - should query with the exact workspaceId and userId provided"() {
        given:
        def workspaceId = 5L
        def userId = 10L
        membershipRepository.findMember(workspaceId, userId) >> Optional.of(Stub(WorkspaceMember))

        when:
        service.verifyUserIsMemberOfWorkspace(workspaceId, userId)

        then:
        1 * membershipRepository.findMember(5L, 10L) >> Optional.of(Stub(WorkspaceMember))
    }

    def "getAllWorkspaceDetails - should mark workspace as default when it matches DEFAULT_WORKSPACE setting"() {
        given:
        def owner = User.builder().id(1L).email("user@test.com").build()
        def proj1 = Stub(WorkspaceSummaryProjection) {
            getAccountId() >> 10L
            getAccountName() >> "Hogar"
            getMembersCount() >> 2L
        }
        def proj2 = Stub(WorkspaceSummaryProjection) {
            getAccountId() >> 20L
            getAccountName() >> "Viajes"
            getMembersCount() >> 1L
        }

        userService.getAuthenticatedUser() >> owner
        userSettingService.getDefaultWorkspaceId(owner) >> Optional.of(10L)
        workspaceRepository.findWorkspaceSummariesByMemberUserId(1L) >> [proj1, proj2]

        when:
        List<WorkspaceDetail> result = service.getAllWorkspaceDetails()

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
        def owner = User.builder().id(1L).email("user@test.com").build()
        def proj = Stub(WorkspaceSummaryProjection) {
            getAccountId() >> 10L
            getAccountName() >> "Hogar"
            getMembersCount() >> 1L
        }

        userService.getAuthenticatedUser() >> owner
        userSettingService.getDefaultWorkspaceId(owner) >> Optional.empty()
        workspaceRepository.findWorkspaceSummariesByMemberUserId(1L) >> [proj]

        when:
        List<WorkspaceDetail> result = service.getAllWorkspaceDetails()

        then:
        result.size() == 1
        result[0].isDefault() == false
    }
}
