package api.expenses.expenses.services.accounts;

import api.expenses.expenses.mappers.AccountMapper;
import api.expenses.expenses.records.accounts.AccountRecord;
import api.expenses.expenses.records.accounts.AccountsWithUser;
import api.expenses.expenses.repositories.AccountRepository;
import api.expenses.expenses.services.user.UserService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        return accountRepository.findAllAccountsByMemberId(owner.getId())
                .stream().map(accountMapper::toRecord)
                .toList();
    }

    public List<AccountsWithUser> getAllAccountsWithUserCount() {
        var owner = userService.getAuthenticatedUser();
        return accountRepository.findAllAccountsByMemberId(owner.getId())
                .stream().map(account -> new AccountsWithUser(account.getId(), account.getName(), account.getMembers().size()))
                .toList();
    }

    public boolean verifyAccountExist(@NotNull String name, Long id) {
        return accountRepository.findAccountByNameAndOwnerId(name, id)
                .isPresent();
    }
}