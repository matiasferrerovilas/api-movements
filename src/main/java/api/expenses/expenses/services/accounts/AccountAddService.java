package api.expenses.expenses.services.accounts;

import api.expenses.expenses.entities.Account;
import api.expenses.expenses.entities.AccountMember;
import api.expenses.expenses.enums.AccountRole;
import api.expenses.expenses.exceptions.PermissionDeniedException;
import api.expenses.expenses.mappers.AccountMapper;
import api.expenses.expenses.records.groups.AddGroupRecord;
import api.expenses.expenses.repositories.AccountMemberRepository;
import api.expenses.expenses.repositories.AccountRepository;
import api.expenses.expenses.services.publishing.websockets.AccountPublishServiceWebSocket;
import api.expenses.expenses.services.user.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountAddService {
    private final AccountQueryService accountQueryService;
    private final UserService userService;
    private final AccountRepository accountRepository;
    private final AccountMemberRepository accountMemberRepository;
    private final AccountPublishServiceWebSocket accountPublishServiceWebSocket;
    private final AccountMapper accountMapper;

    @Transactional
    public void createAccount(AddGroupRecord record) {
        if (StringUtils.isAllBlank(record.description())) {
            log.error("Description for group is blank");
            return;
        }
        var owner = userService.getAuthenticatedUser();

        var account = Account.builder()
                .name(record.description())
                .owner(owner)
                .build();

        var existingAccount = accountQueryService.verifyAccountExist(account.getName(), account.getOwner().getId());
        if (existingAccount) {
            return;
        }
        var membership = AccountMember.builder()
                .user(owner)
                .account(account)
                .role(AccountRole.OWNER)
                .build();
        account.getMembers().add(membership);
        account = accountRepository.save(account);

        accountPublishServiceWebSocket.publishAccountCreated(accountMapper.toRecord(account));
    }

    public void leaveAccount(Long accountId) {
        var user = userService.getAuthenticatedUserRecord();

        var membership = accountMemberRepository
                .findMember(accountId, user.id())
                .orElseThrow(() -> new PermissionDeniedException("User does not belong to this account"));

        if (membership.getRole() == AccountRole.OWNER) {
            throw new PermissionDeniedException("Owner cannot leave the account");
        }

        accountMemberRepository.delete(membership);

        //accountPublishServiceWebSocket.publishAccountCreated(accountMapper.toRecord(account));
    }

    public void addMemberToAccount(Account account) {
        var user = userService.getAuthenticatedUser();
        var membership = AccountMember.builder()
                .user(user)
                .account(account)
                .role(AccountRole.COLLABORATOR)
                .build();

        account.getMembers().add(membership);
        accountMemberRepository.save(membership);
    }

   /* @PublishMovement(eventType = EventType.ACCOUNT_LEFT, routingKey = "/topic/groups/update")
    public List<AccountsWithUser> leaveAccount(Long accountId) throws AccessDeniedException {
        var user = userService.getAuthenticatedUserRecord();

        var membership = accountMemberRepository
                .findByAccountIdAndUserId(accountId, user.id())
                .orElseThrow(() -> new AccessDeniedException("User does not belong to this account"));

        if (membership.getRole() == AccountRole.OWNER) {
            throw new AccessDeniedException("Owner cannot leave the account");
        }

        accountMemberRepository.delete(membership);

        return accountQueryService.getMyAccountsWithCount();
    }*/
}