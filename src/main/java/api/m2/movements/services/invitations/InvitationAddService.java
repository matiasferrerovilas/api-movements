package api.m2.movements.services.invitations;

import api.m2.movements.entities.WorkspaceInvitation;
import api.m2.movements.enums.InvitationStatus;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.exceptions.PermissionDeniedException;
import api.m2.movements.mappers.WorkspaceInvitationMapper;
import api.m2.movements.records.invite.InvitationResponseRecord;
import api.m2.movements.repositories.WorkspaceInvitationRepository;
import api.m2.movements.services.publishing.websockets.WorkspacePublishServiceWebSocket;
import api.m2.movements.services.user.UserService;
import api.m2.movements.services.workspaces.WorkspaceAddService;
import api.m2.movements.services.workspaces.WorkspaceQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationAddService {

    private final WorkspaceAddService workspaceAddService;
    private final WorkspaceQueryService workspaceQueryService;
    private final UserService userService;
    private final WorkspaceInvitationRepository workspaceInvitationRepository;
    private final WorkspaceInvitationMapper workspaceInvitationMapper;
    private final WorkspacePublishServiceWebSocket workspacePublishServiceWebSocket;

    @Transactional
    public void inviteToWorkspace(Long workspaceId, List<String> emails) {
        var workspaceToInvite = workspaceQueryService.findWorkspaceById(workspaceId);

        var loggedInUser = userService.getAuthenticatedUser();
        workspaceQueryService.verifyUserIsMemberOfWorkspace(workspaceToInvite.getId(), loggedInUser.getId());

        var usersToInvite = userService.getUserByEmail(emails);

        if (usersToInvite.isEmpty()) {
            log.info("No se crearon nuevas invitaciones: no se encontraron usuarios con los emails proporcionados.");
            return;
        }
        var pendingInvitations = workspaceInvitationRepository
                .findAllByWorkspaceIdAndStatus(workspaceToInvite.getId(), InvitationStatus.PENDING);

        var alreadyInvitedUserIds = pendingInvitations.stream()
                .map(inv -> inv.getUser().getId())
                .collect(Collectors.toSet());

        var newInvitations = usersToInvite.stream()
                .filter(user -> !alreadyInvitedUserIds.contains(user.getId()))
                .map(user -> WorkspaceInvitation.builder()
                        .user(user)
                        .workspace(workspaceToInvite)
                        .invitedBy(loggedInUser)
                        .status(InvitationStatus.PENDING)
                        .build())
                .toList();

        if (newInvitations.isEmpty()) {
            log.info("No se crearon nuevas invitaciones: todos los usuarios ya tienen invitación pendiente o aceptada.");
            return;
        }

        workspaceInvitationRepository.saveAll(newInvitations);

        newInvitations
                .stream()
                .map(workspaceInvitationMapper::toRecord)
                .forEach(workspacePublishServiceWebSocket::publishInvitationAdded);
    }

    @Transactional
    public void acceptRejectInvitation(Long invitationId, InvitationResponseRecord invitationResponseRecord) {
        var invitation = workspaceInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new EntityNotFoundException("No existe invitación con ese id"));

        var currentUser = userService.getAuthenticatedUser();
        if (!invitation.getUser().getId().equals(currentUser.getId())) {
            throw new PermissionDeniedException("Esta invitación no te pertenece");
        }

        if (!invitation.getStatus().equals(InvitationStatus.PENDING)) {
            log.error("La invitación no esta en estado PENDING");
            return;
        }

        var status = invitationResponseRecord.status() ? InvitationStatus.ACCEPTED : InvitationStatus.REJECTED;
        invitation.setStatus(status);
        workspaceInvitationRepository.save(invitation);

        if (status == InvitationStatus.ACCEPTED) {
            workspaceAddService.addMemberToWorkspace(invitation.getWorkspace());
        }

        workspacePublishServiceWebSocket.publishInvitationUpdated(workspaceInvitationMapper.toRecord(invitation));
    }
}
