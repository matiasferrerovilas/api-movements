package api.m2.movements.records.workspaces;

public record WorkspacesWithUser(Long id,
                                 String name,
                                 long membersCount,
                                 String owner) {
}
