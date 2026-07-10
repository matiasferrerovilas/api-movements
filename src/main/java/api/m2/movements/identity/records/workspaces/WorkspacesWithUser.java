package api.m2.movements.identity.records.workspaces;

public record WorkspacesWithUser(Long id,
                                 String name,
                                 long membersCount,
                                 String owner) {
}
