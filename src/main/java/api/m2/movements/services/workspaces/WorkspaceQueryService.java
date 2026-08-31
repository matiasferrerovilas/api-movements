package api.m2.movements.services.workspaces;

import api.m2.movements.clients.identity.IdentityClient;
import api.m2.movements.clients.identity.requests.AcceptRejectInvitationDTO;
import api.m2.movements.clients.identity.requests.WorkspaceSendInvitationDTO;
import api.m2.movements.configuration.CacheConfiguration;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.exceptions.PermissionDeniedException;
import api.m2.movements.clients.identity.response.WorkspaceInvitationDTO;
import api.m2.movements.clients.identity.response.WorkspaceSentInvitationDTO;
import api.m2.movements.clients.identity.response.WorkspaceMemberDTO;
import api.m2.movements.enums.WorkspaceRole;
import api.m2.movements.services.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceQueryService {
    private final IdentityClient identityClient;
    private final UserService userService;

    public List<WorkspaceMemberDTO> getWorkspaces() {
        return identityClient.getWorkspaces();
    }

    // Cacheado por 5hs (ver CacheConfiguration.IDENTITY_CACHE). @Cacheable en un método void solo
    // cachea la ejecución exitosa (Spring no cachea si el método tira excepción) — una membership
    // confirmada se recuerda por la ventana completa, pero un rechazo siempre se re-verifica
    // contra api-identity, así que sacar a alguien de un workspace corta su acceso de inmediato,
    // no recién cuando expire el cache.
    @Cacheable(cacheNames = CacheConfiguration.IDENTITY_CACHE, key = "'membership:' + #workspaceId + ':' + #userId")
    public void verifyUserIsMemberOfWorkspace(Long workspaceId, Long userId) {
        try {
            identityClient.verifyMembership(workspaceId, userId);
        } catch (RestClientResponseException e) {
            throw new PermissionDeniedException("No tienes permiso para operar sobre este recurso");
        }
    }

    // No usa @Cacheable propio: delega en userService.getMe(workspaceId), que ya está cacheado
    // 5hs (ver CacheConfiguration.IDENTITY_CACHE), así que no agrega un round-trip nuevo a
    // api-identity. El userId no se recibe como parámetro porque getMe(workspaceId) siempre
    // resuelve al caller autenticado vía SecurityContext — coincide con el "userId" que cada
    // call site ya venía resolviendo de la misma forma antes de pasarlo acá.
    public void verifyCanWrite(Long workspaceId) {
        WorkspaceRole role = userService.getMe(workspaceId).metadata().workspaceRole();

        if (role == null) {
            throw new PermissionDeniedException("No tienes permiso para operar sobre este recurso");
        }
        if (role == WorkspaceRole.READ_ONLY) {
            throw new PermissionDeniedException("Los miembros de solo lectura no pueden crear ni modificar recursos");
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

    public List<WorkspaceSentInvitationDTO> getSentInvitations() {
        return identityClient.getSentInvitations();
    }

    public void cancelInvitation(Long invitationId) {
        identityClient.cancelInvitation(invitationId);
    }
}
