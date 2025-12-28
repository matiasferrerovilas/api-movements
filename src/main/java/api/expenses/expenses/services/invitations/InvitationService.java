package api.expenses.expenses.services.invitations;

import api.expenses.expenses.entities.AccountInvitation;
import api.expenses.expenses.enums.InvitationStatus;
import api.expenses.expenses.mappers.AccountInvitationMapper;
import api.expenses.expenses.records.accounts.AccountInvitationRecord;
import api.expenses.expenses.records.groups.InvitationResponseRecord;
import api.expenses.expenses.repositories.AccountInvitationRepository;
import api.expenses.expenses.repositories.AccountRepository;
import api.expenses.expenses.services.accounts.AccountAddService;
import api.expenses.expenses.services.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationService {

    private final AccountAddService accountAddService;
    private final AccountRepository accountRepository;
    private final UserService userService;
    private final AccountInvitationRepository accountInvitationRepository;
    private final AccountInvitationMapper accountInvitationMapper;

    public void inviteToAccount(Long accountId, List<String> emails) {
        var accountToInvite = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account no encontrada"));

        var loggedInUser = userService.getAuthenticatedUser();
        var usersToInvite = userService.getUserByEmail(emails);

        if (usersToInvite.isEmpty()) {
            log.info("No se crearon nuevas invitaciones: no se encontraron usuarios con los emails proporcionados.");
            return;
        }
        var pendingInvitations = accountInvitationRepository
                .findAllByAccountIdAndStatus(accountToInvite.getId(), InvitationStatus.PENDING);

        var alreadyInvitedUserIds = pendingInvitations.stream()
                .map(inv -> inv.getUser().getId())
                .collect(Collectors.toSet());

        var newInvitations = usersToInvite.stream()
                .filter(user -> !alreadyInvitedUserIds.contains(user.getId()))
                .map(user -> AccountInvitation.builder()
                        .user(user)
                        .account(accountToInvite)
                        .invitedBy(loggedInUser)
                        .status(InvitationStatus.PENDING)
                        .build())
                .toList();

        if (newInvitations.isEmpty()) {
            log.info("No se crearon nuevas invitaciones: todos los usuarios ya tienen invitación pendiente o aceptada.");
            return;
        }

        accountInvitationRepository.saveAll(newInvitations);
    }

    public List<AccountInvitationRecord> getAllInvitations() {
        var user = userService.getAuthenticatedUserRecord();
        return accountInvitationMapper.toRecord(accountInvitationRepository.findAllByUserIdAndStatus(user.id(), InvitationStatus.PENDING));
    }

    @Transactional
    public void acceptRejectInvitation(Long invitationId, InvitationResponseRecord invitationResponseRecord) {
        var invitation = accountInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new EntityNotFoundException("No existe invitación con ese id"));

        if (!invitation.getStatus().equals(InvitationStatus.PENDING)) {
            log.error("La invitación no esta en estado PENDING");
            return;
        }

        var status = invitationResponseRecord.status() ? InvitationStatus.ACCEPTED : InvitationStatus.REJECTED;
        invitation.setStatus(status);
        accountInvitationRepository.save(invitation);

        if (status == InvitationStatus.ACCEPTED) {
            accountAddService.addMemberToAccount(invitation.getAccount());
        }
    }
}
