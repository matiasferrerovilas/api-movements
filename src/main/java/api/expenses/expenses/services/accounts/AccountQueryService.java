package api.expenses.expenses.services.accounts;

import api.expenses.expenses.entities.Account;
import api.expenses.expenses.mappers.AccountMapper;
import api.expenses.expenses.records.accounts.AccountRecord;
import api.expenses.expenses.records.accounts.AccountsWithUser;
import api.expenses.expenses.repositories.AccountRepository;
import api.expenses.expenses.services.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
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

    public List<AccountRecord> findAllAccountsOfLogInUser() {
        var owner = userService.getAuthenticatedUser();
        return accountRepository.findAllAccountsByMemberIdWithAllMembers(owner.getId())
                .stream().map(accountMapper::toRecord)
                .toList();
    }


    @Transactional
    public List<AccountsWithUser> getAllAccountsWithUserCount() {
        var owner = userService.getAuthenticatedUser();
        return accountRepository.findAccountSummariesByMemberUserId(owner.getId())
                .stream().map(account -> new AccountsWithUser(account.getAccountId(), account.getAccountName(), account.getMembersCount(), account.getOwnerEmail()))
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
        return accountRepository.findAccountByNameAndOwnerId(name,owner.getId())
                .orElseThrow(() -> new EntityNotFoundException("No existe account con ese nombre en ese usuario"));
    }
}