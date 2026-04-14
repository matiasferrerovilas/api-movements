package api.m2.movements.unit.services

import api.m2.movements.entities.User
import api.m2.movements.entities.Workspace
import api.m2.movements.entities.WorkspaceInvitation
import api.m2.movements.enums.InvitationStatus
import api.m2.movements.mappers.WorkspaceInvitationMapper
import api.m2.movements.records.users.UserBaseRecord
import api.m2.movements.repositories.WorkspaceInvitationRepository
import api.m2.movements.services.invitations.InvitationQueryService
import api.m2.movements.services.user.UserService
import org.mapstruct.factory.Mappers
import spock.lang.Specification

class InvitationQueryServiceTest extends Specification {

    WorkspaceInvitationMapper workspaceInvitationMapper = Mappers.getMapper(WorkspaceInvitationMapper)
    WorkspaceInvitationRepository workspaceInvitationRepository = Mock(WorkspaceInvitationRepository)
    UserService userService = Mock(UserService)

    InvitationQueryService service

    def setup() {
        service = new InvitationQueryService(
                userService,
                workspaceInvitationRepository,
                workspaceInvitationMapper
        )
    }

    def "getAllInvitations - should return pending invitations for authenticated user"() {
        given:
        def userId = 1L
        def userRecord = new UserBaseRecord("test@test.com", userId)

        def inviter = Stub(User) { getId() >> 2L; getEmail() >> "inviter@test.com" }
        def invitedUser = Stub(User) { getId() >> userId; getEmail() >> "test@test.com" }
        def workspace = Stub(Workspace) { getId() >> 10L; getName() >> "Shared Workspace" }

        def invitation = new WorkspaceInvitation(
                id: 1L,
                workspace: workspace,
                user: invitedUser,
                invitedBy: inviter,
                status: InvitationStatus.PENDING
        )

        userService.getAuthenticatedUserRecord() >> userRecord
        workspaceInvitationRepository.findAllByUserIdAndStatus(userId, InvitationStatus.PENDING) >> [invitation]

        when:
        def result = service.getAllInvitations()

        then:
        result.size() == 1
        result[0].workspaceName() == "Shared Workspace"
        result[0].invitedBy() == "inviter@test.com"
    }

    def "getAllInvitations - should return empty list when no pending invitations"() {
        given:
        def userId = 1L
        def userRecord = new UserBaseRecord("test@test.com", userId)

        userService.getAuthenticatedUserRecord() >> userRecord
        workspaceInvitationRepository.findAllByUserIdAndStatus(userId, InvitationStatus.PENDING) >> []

        when:
        def result = service.getAllInvitations()

        then:
        result.isEmpty()
    }

    def "getAllInvitations - should return multiple pending invitations"() {
        given:
        def userId = 1L
        def userRecord = new UserBaseRecord("test@test.com", userId)

        def inviter1 = Stub(User) { getId() >> 2L; getEmail() >> "user1@test.com" }
        def inviter2 = Stub(User) { getId() >> 3L; getEmail() >> "user2@test.com" }
        def invitedUser = Stub(User) { getId() >> userId; getEmail() >> "test@test.com" }
        def workspace1 = Stub(Workspace) { getId() >> 10L; getName() >> "Workspace 1" }
        def workspace2 = Stub(Workspace) { getId() >> 20L; getName() >> "Workspace 2" }

        def invitation1 = new WorkspaceInvitation(
                id: 1L,
                workspace: workspace1,
                user: invitedUser,
                invitedBy: inviter1,
                status: InvitationStatus.PENDING
        )
        def invitation2 = new WorkspaceInvitation(
                id: 2L,
                workspace: workspace2,
                user: invitedUser,
                invitedBy: inviter2,
                status: InvitationStatus.PENDING
        )

        userService.getAuthenticatedUserRecord() >> userRecord
        workspaceInvitationRepository.findAllByUserIdAndStatus(userId, InvitationStatus.PENDING) >> [invitation1, invitation2]

        when:
        def result = service.getAllInvitations()

        then:
        result.size() == 2
        result.collect { it.workspaceName() } as Set == ["Workspace 1", "Workspace 2"] as Set
    }
}
