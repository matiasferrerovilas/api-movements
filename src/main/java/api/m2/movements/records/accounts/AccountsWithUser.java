package api.m2.movements.records.accounts;

public record AccountsWithUser(Long accountId,
                               String name,
                               long membersCount,
                               String owner) {
}
