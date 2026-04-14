package api.m2.movements.unit.services

import api.m2.movements.entities.User
import api.m2.movements.entities.Workspace
import api.m2.movements.entities.WorkspaceMember
import api.m2.movements.enums.WorkspaceRole
import api.m2.movements.exceptions.BusinessException
import api.m2.movements.exceptions.PermissionDeniedException
import api.m2.movements.mappers.WorkspaceMapper
import api.m2.movements.records.workspaces.AddWorkspaceRecord
import api.m2.movements.records.workspaces.WorkspaceDetail
import api.m2.movements.records.workspaces.WorkspaceRecord
import api.m2.movements.records.users.UserBaseRecord
import api.m2.movements.repositories.MembershipRepository
import api.m2.movements.repositories.WorkspaceRepository
import api.m2.movements.services.publishing.websockets.WorkspacePublishServiceWebSocket
import api.m2.movements.services.settings.UserSettingService
import api.m2.movements.services.user.UserService
import api.m2.movements.services.workspaces.WorkspaceAddService
import api.m2.movements.services.workspaces.WorkspaceQueryService
import spock.lang.Specification

class WorkspaceAddServiceTest extends Specification {

    WorkspaceQueryService workspaceQueryService = Mock(WorkspaceQueryService)
    UserService userService = Mock(UserService)
    WorkspaceRepository workspaceRepository = Mock(WorkspaceRepository)
    MembershipRepository membershipRepository = Mock(MembershipRepository)
    WorkspacePublishServiceWebSocket workspacePublishServiceWebSocket = Mock(WorkspacePublishServiceWebSocket)
    WorkspaceMapper workspaceMapper = Mock(WorkspaceMapper)
    UserSettingService userSettingService = Mock(UserSettingService)

    WorkspaceAddService service

    def setup() {
        service = new WorkspaceAddService(
                workspaceQueryService,
                userService,
                workspaceRepository,
                membershipRepository,
                workspacePublishServiceWebSocket,
                workspaceMapper,
                userSettingService
        )
    }

    def "createWorkspace - should save workspace and publish WorkspaceDetail via publishWorkspaceMembershipUpdated"() {
        given:
        def record = new AddWorkspaceRecord("Viajes")
        def owner = User.builder().id(1L).email("user@test.com").build()
        def savedWorkspace = Workspace.builder().id(10L).name("Viajes").owner(owner).build()

        userService.getAuthenticatedUser() >> owner
        userService.getCurrentKeycloakId() >> "keycloak-uuid-123"
        workspaceQueryService.verifyWorkspaceExist("Viajes", 1L) >> false
        workspaceRepository.save(_ as Workspace) >> savedWorkspace

        when:
        service.createWorkspace(record)

        then:
        1 * workspaceRepository.save(_ as Workspace) >> savedWorkspace
        1 * workspacePublishServiceWebSocket.publishWorkspaceMembershipUpdated(
                new WorkspaceDetail(10L, "Viajes", 1, false),
                "keycloak-uuid-123"
        )
    }

    def "createWorkspace - should throw BusinessException when description is blank"() {
        given:
        def record = new AddWorkspaceRecord("   ")

        when:
        service.createWorkspace(record)

        then:
        thrown(BusinessException)
        0 * workspaceRepository.save(_ as Workspace)
        0 * workspacePublishServiceWebSocket.publishWorkspaceMembershipUpdated(_ as WorkspaceDetail, _ as String)
    }

    def "createWorkspace - should throw BusinessException when workspace already exists"() {
        given:
        def record = new AddWorkspaceRecord("Hogar")
        def owner = User.builder().id(2L).email("user@test.com").build()

        userService.getAuthenticatedUser() >> owner
        workspaceQueryService.verifyWorkspaceExist("Hogar", 2L) >> true

        when:
        service.createWorkspace(record)

        then:
        thrown(BusinessException)
        0 * workspaceRepository.save(_ as Workspace)
        0 * workspacePublishServiceWebSocket.publishWorkspaceMembershipUpdated(_ as WorkspaceDetail, _ as String)
    }

    def "leaveWorkspace - should throw PermissionDeniedException when user is not a member"() {
        given:
        def user = new UserBaseRecord("John", 5L)

        userService.getAuthenticatedUserRecord() >> user
        membershipRepository.findMember(99L, 5L) >> Optional.empty()

        when:
        service.leaveWorkspace(99L)

        then:
        thrown(PermissionDeniedException)
    }

    def "leaveWorkspace - should throw PermissionDeniedException when owner tries to leave with other members"() {
        given:
        def user = new UserBaseRecord("Owner", 1L)
        def workspace = Workspace.builder().id(10L).name("Grupo").build()
        def membership = Stub(WorkspaceMember) {
            getRole() >> WorkspaceRole.OWNER
            getWorkspace() >> workspace
        }

        userService.getAuthenticatedUserRecord() >> user
        membershipRepository.findMember(10L, 1L) >> Optional.of(membership)
        membershipRepository.countByWorkspaceId(10L) >> 3L

        when:
        service.leaveWorkspace(10L)

        then:
        thrown(PermissionDeniedException)
    }

    def "leaveWorkspace - should deactivate workspace and publish event when owner leaves as sole member"() {
        given:
        def user = new UserBaseRecord("Owner", 1L)
        def workspace = Workspace.builder().id(10L).name("Solo").build()
        def membership = Stub(WorkspaceMember) {
            getRole() >> WorkspaceRole.OWNER
            getWorkspace() >> workspace
        }
        def workspaceRecord = new WorkspaceRecord(10L, "Solo", new UserBaseRecord("Owner", 1L), [])

        userService.getAuthenticatedUserRecord() >> user
        membershipRepository.findMember(10L, 1L) >> Optional.of(membership)
        membershipRepository.countByWorkspaceId(10L) >> 1L
        workspaceMapper.toRecord(workspace) >> workspaceRecord

        when:
        service.leaveWorkspace(10L)

        then:
        workspace.isActive() == false
        1 * workspaceRepository.save(workspace)
        1 * membershipRepository.delete(membership)
        1 * workspacePublishServiceWebSocket.publishWorkspaceLeft(workspaceRecord)
    }

    def "leaveWorkspace - should delete membership and publish event for collaborator"() {
        given:
        def user = new UserBaseRecord("Collab", 2L)
        def workspace = Workspace.builder().id(10L).name("Grupo").build()
        def membership = Stub(WorkspaceMember) {
            getRole() >> WorkspaceRole.COLLABORATOR
            getWorkspace() >> workspace
        }
        def workspaceRecord = new WorkspaceRecord(10L, "Grupo", new UserBaseRecord("Owner", 1L), [])

        userService.getAuthenticatedUserRecord() >> user
        membershipRepository.findMember(10L, 2L) >> Optional.of(membership)
        workspaceMapper.toRecord(workspace) >> workspaceRecord

        when:
        service.leaveWorkspace(10L)

        then:
        0 * workspaceRepository.save(_ as Workspace)
        1 * membershipRepository.delete(membership)
        1 * workspacePublishServiceWebSocket.publishWorkspaceLeft(workspaceRecord)
    }

    def "addMemberToWorkspace - should publish MEMBERSHIP_UPDATED after member is saved"() {
        given:
        def owner = User.builder().id(1L).email("owner@test.com").build()
        def workspace = Workspace.builder().id(20L).name("Familia").owner(owner).build()
        def joiningUser = User.builder().id(2L).email("new@test.com").build()

        userService.getAuthenticatedUser() >> joiningUser
        membershipRepository.countByWorkspaceId(20L) >> 3L

        when:
        service.addMemberToWorkspace(workspace)

        then:
        1 * membershipRepository.save(_ as WorkspaceMember)
        1 * workspacePublishServiceWebSocket.publishMemberAdded(
                new WorkspaceDetail(20L, "Familia", 3, false),
                20L
        )
    }

    def "updateDefaultWorkspace - should upsert setting and publish WorkspaceDetail with isDefault true"() {
        given:
        def user = new UserBaseRecord("User", 1L)
        def workspace = Workspace.builder().id(30L).name("Principal").build()
        def membership = Stub(WorkspaceMember) {
            getWorkspace() >> workspace
        }

        userService.getAuthenticatedUserRecord() >> user
        userService.getCurrentKeycloakId() >> "keycloak-uuid-456"
        membershipRepository.findMember(30L, 1L) >> Optional.of(membership)
        membershipRepository.countByWorkspaceId(30L) >> 2L

        when:
        service.updateDefaultWorkspace(30L)

        then:
        1 * userSettingService.upsert(_ as api.m2.movements.enums.UserSettingKey, 30L)
        1 * workspacePublishServiceWebSocket.publishWorkspaceMembershipUpdated(
                new WorkspaceDetail(30L, "Principal", 2, true),
                "keycloak-uuid-456"
        )
    }

    def "updateDefaultWorkspace - should throw PermissionDeniedException when user is not a member"() {
        given:
        def user = new UserBaseRecord("User", 1L)

        userService.getAuthenticatedUserRecord() >> user
        membershipRepository.findMember(99L, 1L) >> Optional.empty()

        when:
        service.updateDefaultWorkspace(99L)

        then:
        thrown(PermissionDeniedException)
    }
}
