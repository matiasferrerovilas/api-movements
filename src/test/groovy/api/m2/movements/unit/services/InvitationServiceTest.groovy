package api.m2.movements.unit.services

import api.m2.movements.entities.User
import api.m2.movements.entities.Workspace
import api.m2.movements.entities.WorkspaceInvitation
import api.m2.movements.enums.InvitationStatus
import api.m2.movements.exceptions.PermissionDeniedException
import api.m2.movements.mappers.WorkspaceInvitationMapper
import api.m2.movements.records.invite.InvitationResponseRecord
import api.m2.movements.records.invite.InvitationToWorkspaceRecord
import api.m2.movements.repositories.WorkspaceInvitationRepository
import api.m2.movements.services.workspaces.WorkspaceAddService
import api.m2.movements.services.workspaces.WorkspaceQueryService
import api.m2.movements.services.invitations.InvitationService
import api.m2.movements.services.publishing.websockets.WorkspacePublishServiceWebSocket
import api.m2.movements.services.user.UserService
import spock.lang.Specification

class InvitationServiceTest extends Specification {

    WorkspaceAddService workspaceAddService = Mock()
    UserService userService = Mock()
    WorkspaceInvitationRepository workspaceInvitationRepository = Mock()
    WorkspacePublishServiceWebSocket workspacePublishServiceWebSocket = Mock()
    WorkspaceQueryService workspaceQueryService = Mock()
    WorkspaceInvitationMapper workspaceInvitationMapper = Mock()

    InvitationService service

    def setup() {
        service = new InvitationService(
                workspaceAddService,
                workspaceQueryService,
                userService,
                workspaceInvitationRepository,
                workspaceInvitationMapper,
                workspacePublishServiceWebSocket
        )
    }

    // --- inviteToAccount ---

    def "inviteToAccount - should throw PermissionDeniedException when caller is not member of workspace"() {
        given:
        def caller = User.builder().id(10L).email("caller@test.com").build()
        def workspace = Workspace.builder().id(1L).name("Mi cuenta").owner(caller).build()

        workspaceQueryService.findWorkspaceById(1L) >> workspace
        userService.getAuthenticatedUser() >> caller
        workspaceQueryService.verifyUserIsMemberOfWorkspace(1L, 10L) >> {
            throw new PermissionDeniedException("No tienes permiso para operar sobre este recurso")
        }

        when:
        service.inviteToAccount(1L, ["invited@test.com"])

        then:
        thrown(PermissionDeniedException)
        0 * workspaceInvitationRepository.saveAll(_ as List)
    }

    def "inviteToAccount - should create invitations when caller is member of workspace"() {
        given:
        def caller = User.builder().id(10L).email("caller@test.com").build()
        def workspace = Workspace.builder().id(1L).name("Mi cuenta").owner(caller).build()
        def invited = User.builder().id(20L).email("invited@test.com").build()
        def invitationRecord = Stub(InvitationToWorkspaceRecord)

        workspaceQueryService.findWorkspaceById(1L) >> workspace
        userService.getAuthenticatedUser() >> caller
        workspaceQueryService.verifyUserIsMemberOfWorkspace(1L, 10L) >> {}
        userService.getUserByEmail(["invited@test.com"]) >> [invited]
        workspaceInvitationRepository.findAllByWorkspaceIdAndStatus(1L, InvitationStatus.PENDING) >> []
        workspaceInvitationRepository.saveAll(_ as List) >> []
        workspaceInvitationMapper.toRecord(_ as WorkspaceInvitation) >> invitationRecord

        when:
        service.inviteToAccount(1L, ["invited@test.com"])

        then:
        1 * workspaceInvitationRepository.saveAll(_ as List)
        1 * workspacePublishServiceWebSocket.publishInvitationAdded(_ as InvitationToWorkspaceRecord)
    }

    // --- acceptRejectInvitation ---

    def "acceptRejectInvitation - should throw PermissionDeniedException when invitation does not belong to current user"() {
        given:
        def owner = User.builder().id(99L).email("owner@test.com").build()
        def currentUser = User.builder().id(10L).email("other@test.com").build()
        def invitation = WorkspaceInvitation.builder()
                .id(5L)
                .user(owner)
                .status(InvitationStatus.PENDING)
                .build()

        workspaceInvitationRepository.findById(5L) >> Optional.of(invitation)
        userService.getAuthenticatedUser() >> currentUser

        when:
        service.acceptRejectInvitation(5L, new InvitationResponseRecord(5L, true))

        then:
        thrown(PermissionDeniedException)
        0 * workspaceInvitationRepository.save(_ as WorkspaceInvitation)
    }

    def "acceptRejectInvitation - should accept invitation when it belongs to current user"() {
        given:
        def currentUser = User.builder().id(10L).email("user@test.com").build()
        def workspace = Workspace.builder().id(1L).name("Mi cuenta").owner(currentUser).build()
        def invitation = WorkspaceInvitation.builder()
                .id(5L)
                .user(currentUser)
                .workspace(workspace)
                .status(InvitationStatus.PENDING)
                .build()
        def invitationRecord = Stub(InvitationToWorkspaceRecord)

        workspaceInvitationRepository.findById(5L) >> Optional.of(invitation)
        userService.getAuthenticatedUser() >> currentUser
        workspaceInvitationMapper.toRecord(_ as WorkspaceInvitation) >> invitationRecord

        when:
        service.acceptRejectInvitation(5L, new InvitationResponseRecord(5L, true))

        then:
        1 * workspaceInvitationRepository.save(_ as WorkspaceInvitation)
        1 * workspaceAddService.addMemberToWorkspace(workspace)
        1 * workspacePublishServiceWebSocket.publishInvitationUpdated(_ as InvitationToWorkspaceRecord)
    }
}
