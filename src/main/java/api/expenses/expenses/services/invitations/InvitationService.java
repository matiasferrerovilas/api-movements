package api.expenses.expenses.services.invitations;

import api.expenses.expenses.entities.GroupInvitation;
import api.expenses.expenses.enums.InvitationStatus;
import api.expenses.expenses.repositories.AccountRepository;
import api.expenses.expenses.repositories.AccountInvitationRepository;
import api.expenses.expenses.services.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationService {

    private final AccountRepository accountRepository;
    private final UserService userService;
    private final AccountInvitationRepository accountInvitationRepository;

    public void inviteToAccount(Long accountId, List<String> emails) {
        var accountToInvite = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account no encontrada"));

        var loggedInUser = userService.getAuthenticatedUser();
        var usersToInvite = userService.getUserByEmail(emails);

        if(usersToInvite.isEmpty()){
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
                .map(user -> GroupInvitation.builder()
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
}
