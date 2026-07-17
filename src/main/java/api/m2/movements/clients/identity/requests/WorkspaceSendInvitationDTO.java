package api.m2.movements.clients.identity.requests;

import java.util.List;

public record WorkspaceSendInvitationDTO(Long workspaceId, List<String> emails) {
}
