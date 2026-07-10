package api.m2.movements.identity.records.invite;

public record InvitationToWorkspaceRecord(Long id, String workspaceName, String invitedBy, Long invitedUserId) {
}
