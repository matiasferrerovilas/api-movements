package api.m2.movements.clients.identity.response;

import api.m2.movements.enums.InvitationStatus;
import api.m2.movements.enums.WorkspaceRole;

import java.io.Serializable;
import java.time.LocalDateTime;

public record WorkspaceSentInvitationDTO(
        Long id,
        Long workspaceId,
        String workspaceName,
        String invitedUserEmail,
        InvitationStatus status,
        WorkspaceRole role,
        LocalDateTime createdAt) implements Serializable {
}
