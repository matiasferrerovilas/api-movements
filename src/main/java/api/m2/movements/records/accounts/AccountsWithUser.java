package api.m2.movements.records.accounts;

public record AccountsWithUser(Long id,
                               String name,
                               long membersCount,
                               String owner,
                               boolean isDefault) {
}
