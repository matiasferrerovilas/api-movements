package api.m2.movements.clients.identity.response;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import api.m2.movements.enums.InvitationStatus;
import api.m2.movements.enums.WorkspaceRole;

import java.io.Serializable;
import java.time.LocalDateTime;

@RegisterReflectionForBinding
public record WorkspaceInvitationDTO(
        Long id,
        Long workspaceId,
        String workspaceName,
        String invitedByEmail,
        InvitationStatus status,
        WorkspaceRole role,
        LocalDateTime createdAt) implements Serializable {
}
