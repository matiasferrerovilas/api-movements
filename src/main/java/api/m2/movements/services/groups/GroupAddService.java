package api.m2.movements.services.groups;

import api.m2.movements.entities.Account;
import api.m2.movements.entities.AccountMember;
import api.m2.movements.enums.AccountRole;
import api.m2.movements.enums.UserSettingKey;
import api.m2.movements.exceptions.PermissionDeniedException;
import api.m2.movements.mappers.AccountMapper;
import api.m2.movements.records.accounts.GroupDetail;
import api.m2.movements.records.groups.AddGroupRecord;
import api.m2.movements.repositories.MembershipRepository;
import api.m2.movements.repositories.AccountRepository;
import api.m2.movements.services.publishing.websockets.AccountPublishServiceWebSocket;
import api.m2.movements.services.settings.UserSettingService;
import api.m2.movements.services.user.UserService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupAddService {
    private final AccountQueryService accountQueryService;
    private final UserService userService;
    private final AccountRepository accountRepository;
    private final MembershipRepository membershipRepository;
    private final AccountPublishServiceWebSocket accountPublishServiceWebSocket;
    private final AccountMapper accountMapper;
    private final UserSettingService userSettingService;

    @Transactional
    public void createAccount(AddGroupRecord addGroupRecord) {
        if (StringUtils.isAllBlank(addGroupRecord.description())) {
            log.error("Description for group is blank");
            return;
        }
        var owner = userService.getAuthenticatedUser();

        var account = Account.builder()
                .name(addGroupRecord.description())
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

        var keycloakSubject = userService.getCurrentKeycloakId();
        var groupDetail = new GroupDetail(account.getId(), account.getName(), 1, false);
        accountPublishServiceWebSocket.publishGroupMembershipUpdated(groupDetail, keycloakSubject);
    }

    @Transactional
    public void leaveAccount(Long accountId) {
        var user = userService.getAuthenticatedUserRecord();

        var membership = membershipRepository
                .findMember(accountId, user.id())
                .orElseThrow(() -> new PermissionDeniedException("User does not belong to this account"));

        if (membership.getRole() == AccountRole.OWNER) {
            long memberCount = membershipRepository.countByAccountId(accountId);
            if (memberCount > 1) {
                throw new PermissionDeniedException("Owner cannot leave the account while it has other members");
            }
            var account = membership.getAccount();
            account.setActive(false);
            accountRepository.save(account);
        }

        membershipRepository.delete(membership);
        accountPublishServiceWebSocket.publishAccountLeft(accountMapper.toRecord(membership.getAccount()));
    }

    @Transactional
    public void addMemberToAccount(Account account) {
        var user = userService.getAuthenticatedUser();
        var membership = AccountMember.builder()
                .user(user)
                .account(account)
                .role(AccountRole.COLLABORATOR)
                .build();

        account.getMembers().add(membership);
        membershipRepository.save(membership);

        long membersCount = membershipRepository.countByAccountId(account.getId());
        var groupDetail = new GroupDetail(account.getId(), account.getName(), (int) membersCount, false);
        accountPublishServiceWebSocket.publishMemberAdded(groupDetail, account.getId());
    }

    @Transactional
    public void updateDefaultGroup(Long id) {
        var user = userService.getAuthenticatedUserRecord();
        var keycloakUserId = userService.getCurrentKeycloakId();
        var newDefaultMembership = membershipRepository.findMember(id, user.id())
                .orElseThrow(() -> new PermissionDeniedException("El usuario no pertenece a este grupo"));

        userSettingService.upsert(UserSettingKey.DEFAULT_ACCOUNT, newDefaultMembership.getAccount().getId());

        long membersCount = membershipRepository.countByAccountId(id);
        var account = newDefaultMembership.getAccount();
        var groupDetail = new GroupDetail(account.getId(), account.getName(), (int) membersCount, true);
        accountPublishServiceWebSocket.publishGroupMembershipUpdated(groupDetail, keycloakUserId);
    }
}