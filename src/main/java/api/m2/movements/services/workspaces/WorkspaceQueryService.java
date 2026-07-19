package api.m2.movements.services.workspaces;

import api.m2.movements.clients.identity.IdentityClient;
import api.m2.movements.clients.identity.requests.AcceptRejectInvitationDTO;
import api.m2.movements.clients.identity.requests.WorkspaceSendInvitationDTO;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.exceptions.PermissionDeniedException;
import api.m2.movements.clients.identity.response.WorkspaceInvitationDTO;
import api.m2.movements.clients.identity.response.WorkspaceMemberDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceQueryService {
    private final IdentityClient identityClient;

    public List<WorkspaceMemberDTO> getWorkspaces() {
        return identityClient.getWorkspaces();
    }

    public void verifyUserIsMemberOfWorkspace(Long workspaceId, Long userId) {
        try {
            identityClient.verifyMembership(workspaceId, userId);
        } catch (RestClientResponseException e) {
            throw new PermissionDeniedException("No tienes permiso para operar sobre este recurso");
        }
    }

    public List<WorkspaceInvitationDTO> getMyInvitations() {
        return identityClient.getInvitations();
    }

    public String findWorkspaceNameById(Long workspaceId) {
        return identityClient.getWorkspaces().stream()
                .filter(workspace -> workspaceId.equals(workspace.workspaceId()))
                .map(WorkspaceMemberDTO::workspaceName)
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Workspace no encontrado: " + workspaceId));
    }

    public void sendInvitation(Long workspaceId, @Valid WorkspaceSendInvitationDTO body) {
        identityClient.sendInvitation(workspaceId, body);
    }

    public void acceptRejectInvitation(@Valid AcceptRejectInvitationDTO body) {
        identityClient.acceptRejectInvitation(body);
    }
}
