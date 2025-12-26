package api.expenses.expenses.services.groups;

import api.expenses.expenses.aspect.interfaces.PublishMovement;
import api.expenses.expenses.entities.AccountInvitation;
import api.expenses.expenses.enums.EventType;
import api.expenses.expenses.enums.InvitationStatus;
import api.expenses.expenses.mappers.GroupInvitationMapper;
import api.expenses.expenses.records.groups.GroupInvitationRecord;
import api.expenses.expenses.repositories.AccountInvitationRepository;
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
    private final AccountInvitationRepository accountInvitationRepository;
    private final UserService userService;
    private final GroupRepository groupRepository;
    private final GroupInvitationMapper groupInvitationMapper;
    private final UserRepository userRepository;

    public List<GroupInvitationRecord> getAllInvitations() {
        var user = userService.getAuthenticatedUserRecord();
        return groupInvitationMapper.toRecord(accountInvitationRepository.findAllByUserIdAndStatus(user.id(), InvitationStatus.PENDING));
    }

    @Transactional
    @PublishMovement(eventType = EventType.INVITATION_ADDED, routingKey = "/topic/invitation/new")
    public List<GroupInvitationRecord> inviteToGroup(Long groupId, List<String> emails) {
        var loggedInUser = userService.getAuthenticatedUser();
        var groupToInvite = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("No existe el grupo en cuestion"));

        var usersToInvite = userService.getUserByEmail(emails);

        var pendingInvitations = accountInvitationRepository
                .findAllByAccountIdAndStatus(groupToInvite.getId(), InvitationStatus.PENDING);

        var alreadyInvitedUserIds = pendingInvitations.stream()
                .map(inv -> inv.getUser().getId())
                .collect(Collectors.toSet());


        var newInvitations = usersToInvite.stream()
                .filter(user -> !alreadyInvitedUserIds.contains(user.getId()))
                .map(user -> AccountInvitation.builder()
                        .user(user)
                        //.group(groupToInvite)
                        .invitedBy(loggedInUser)
                        .status(InvitationStatus.PENDING)
                        .build())
                .toList();

        if (newInvitations.isEmpty()) {
            log.info("No se crearon nuevas invitaciones: todos los usuarios ya tienen invitación pendiente o aceptada.");
            return this.getAllInvitations();
        }
        return null;//groupInvitationMapper.toRecord(accountInvitationRepository.saveAll(newInvitations));
    }
}