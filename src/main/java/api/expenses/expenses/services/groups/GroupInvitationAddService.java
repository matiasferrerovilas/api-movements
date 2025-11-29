package api.expenses.expenses.services.groups;

import api.expenses.expenses.aspect.interfaces.PublishMovement;
import api.expenses.expenses.entities.GroupInvitation;
import api.expenses.expenses.enums.EventType;
import api.expenses.expenses.enums.InvitationStatus;
import api.expenses.expenses.mappers.GroupInvitationMapper;
import api.expenses.expenses.records.groups.GroupInvitationRecord;
import api.expenses.expenses.records.groups.InvitationResponseRecord;
import api.expenses.expenses.repositories.GroupInvitationRepository;
import api.expenses.expenses.repositories.GroupRepository;
import api.expenses.expenses.repositories.UserRepository;
import api.expenses.expenses.services.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class GroupInvitationAddService {
    private final GroupInvitationRepository groupInvitationRepository;
    private final UserService userService;
    private final GroupRepository groupRepository;
    private final GroupInvitationMapper groupInvitationMapper;
    private final UserRepository userRepository;

    public List<GroupInvitationRecord> getAllInvitations() {
        var user = userService.getAuthenticatedUserRecord();
        return groupInvitationMapper.toRecord(groupInvitationRepository.findAllByUserIdAndStatus(user.id(), InvitationStatus.PENDING));
    }

    @Transactional
    @PublishMovement(eventType = EventType.INVITATION_ADDED, routingKey = "/topic/invitation/new")
    public List<GroupInvitationRecord> inviteToGroup(Long groupId, List<String> emails) {
        var loggedInUser = userService.getAuthenticatedUser();
        var groupToInvite = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("No existe el grupo en cuestion"));

        var usersToInvite = userService.getUserByEmail(emails);

        var pendingInvitations = groupInvitationRepository
                .findAllByGroupIdAndStatus(groupToInvite.getId(), InvitationStatus.PENDING);

        var alreadyInvitedUserIds = pendingInvitations.stream()
                .map(inv -> inv.getUser().getId())
                .collect(Collectors.toSet());


        var newInvitations = usersToInvite.stream()
                .filter(user -> !alreadyInvitedUserIds.contains(user.getId()))
                .map(user -> GroupInvitation.builder()
                        .user(user)
                        .group(groupToInvite)
                        .invitedBy(loggedInUser)
                        .status(InvitationStatus.PENDING)
                        .build())
                .toList();

        if (newInvitations.isEmpty()) {
            log.info("No se crearon nuevas invitaciones: todos los usuarios ya tienen invitación pendiente o aceptada.");
            return this.getAllInvitations();
        }
        return groupInvitationMapper.toRecord(groupInvitationRepository.saveAll(newInvitations));
    }

    @Transactional
    @PublishMovement(eventType = EventType.INVITATION_CONFIRMED_REJECTED, routingKey = "/topic/invitation/update")
    public List<GroupInvitationRecord> acceptRejectInvitation(Long invitationId, InvitationResponseRecord confirmInvitations) {
        var invitation = groupInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new EntityNotFoundException("No existe invitacion con ese id"));

        if (!invitation.getStatus().equals(InvitationStatus.PENDING)) {
            log.error("La invitacion no esta en estado PENDING");
            return this.getAllInvitations();
        }

        var status = confirmInvitations.status() ? InvitationStatus.ACCEPTED : InvitationStatus.REJECTED;
        invitation.setStatus(status);
        groupInvitationRepository.save(invitation);

        if (status == InvitationStatus.ACCEPTED) {
            var user = invitation.getUser();
            var group = invitation.getGroup();
            user.getUserGroups().add(group);
            userRepository.save(user);
        }

        return this.getAllInvitations();
    }
}