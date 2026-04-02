package api.m2.movements.services.groups;

import api.m2.movements.entities.Account;
import api.m2.movements.exceptions.PermissionDeniedException;
import api.m2.movements.mappers.AccountMapper;
import api.m2.movements.records.accounts.GroupDetail;
import api.m2.movements.records.accounts.GroupRecord;
import api.m2.movements.repositories.AccountRepository;
import api.m2.movements.repositories.MembershipRepository;
import api.m2.movements.services.settings.UserSettingService;
import api.m2.movements.services.user.UserService;
import api.m2.movements.exceptions.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountQueryService {
    private final AccountRepository accountRepository;
    private final UserService userService;
    private final AccountMapper accountMapper;
    private final MembershipRepository membershipRepository;
    private final UserSettingService userSettingService;

    public List<GroupRecord> findAllAccountsOfLogInUser() {
        var owner = userService.getAuthenticatedUser();
        return accountRepository.findAllAccountsByMemberIdWithAllMembers(owner.getId())
                .stream().map(accountMapper::toRecord)
                .toList();
    }


    @Transactional
    public List<GroupDetail> getAllGroupDetails() {
        var owner = userService.getAuthenticatedUser();
        var defaultAccountId = userSettingService.getDefaultAccountId(owner).orElse(null);
        return accountRepository.findAccountSummariesByMemberUserId(owner.getId())
                .stream()
                .map(a -> new GroupDetail(
                        a.getAccountId(),
                        a.getAccountName(),
                        a.getMembersCount().intValue(),
                        a.getAccountId().equals(defaultAccountId)))
                .toList();
    }

    public boolean verifyAccountExist(@NotNull String name, Long id) {
        return accountRepository.findAccountByNameAndOwnerId(name, id)
                .isPresent();
    }

    public Account findAccountByName(String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("La descripción del grupo no puede estar vacía");
        }

        var owner = userService.getAuthenticatedUser();
        return accountRepository.findAccountByNameAndOwnerId(name, owner.getId())
                .orElseThrow(() -> new EntityNotFoundException("No existe account con ese nombre en ese usuario"));
    }

    public Account findAccountById(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("No existe account con ese id"));
    }

    public void verifyUserIsMemberOfAccount(Long accountId, Long userId) {
        membershipRepository.findMember(accountId, userId)
                .orElseThrow(() -> new PermissionDeniedException("No tienes permiso para operar sobre este recurso"));
    }
}