package api.m2.movements.clients.identity.response;

import api.m2.movements.enums.WorkspaceRole;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public record WorkspaceMemberDTO(
        Long id,
        Long workspaceId,
        String workspaceName,
        Metadata metadata) implements Serializable {

    public record Metadata(
            List<String> members,
            WorkspaceRole role,
            LocalDateTime joinedAt,
            Boolean isDefault) implements Serializable {
    }
}
