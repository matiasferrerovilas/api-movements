package api.expenses.expenses.records.accounts;

public record AccountsWithUser(Long accountId,
                               String name,
                               long membersCount,
                               String owner) {
}
