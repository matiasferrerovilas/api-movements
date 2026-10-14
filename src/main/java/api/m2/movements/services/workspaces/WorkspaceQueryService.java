package api.m2.movements.services.workspaces;

import api.m2.movements.clients.identity.IdentityGateway;
import api.m2.movements.clients.identity.requests.AcceptRejectInvitationDTO;
import api.m2.movements.clients.identity.requests.WorkspaceSendInvitationDTO;
import api.m2.movements.configuration.CacheConfiguration;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.exceptions.PermissionDeniedException;
import api.m2.movements.exceptions.ServiceException;
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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceQueryService {
    private final IdentityGateway identityGateway;
    private final UserService userService;

    public List<WorkspaceMemberDTO> getWorkspaces() {
        return identityGateway.getWorkspaces();
    }

    // Cacheado por 5hs (ver CacheConfiguration.IDENTITY_CACHE). @Cacheable en un método void solo
    // cachea la ejecución exitosa (Spring no cachea si el método tira excepción): una membership
    // confirmada se recuerda por la ventana completa, y un rechazo se re-verifica siempre contra
    // api-identity (por eso sumar a alguien a un workspace le da acceso de inmediato). El caso
    // contrario NO se cubre solo: sacar a alguien no borra la membership que ya se recordó, así
    // que IdentityCacheInvalidator la invalida cuando alguien sale o lo sacan de un workspace.
    @Cacheable(cacheNames = CacheConfiguration.IDENTITY_CACHE, key = "'membership:' + #workspaceId + ':' + #userId")
    public void verifyUserIsMemberOfWorkspace(Long workspaceId, Long userId) {
        try {
            identityGateway.verifyMembership(workspaceId, userId);
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
        return identityGateway.getInvitations();
    }

    /**
     * Nombre del workspace, solo para mostrar. Cacheado (ver {@link CacheConfiguration#NAMES_CACHE})
     * y best-effort: si api-identity no responde o devuelve un error de servidor devuelve
     * {@code null} (con un warning, y sin cachear) en vez de hacer fallar la operación que lo
     * pidió. Que el workspace no exista para el caller sigue siendo un error.
     */
    @Cacheable(cacheNames = CacheConfiguration.NAMES_CACHE, key = "'workspace:' + #workspaceId", unless = "#result == null")
    public String findWorkspaceNameById(Long workspaceId) {
        try {
            return identityGateway.getWorkspaces().stream()
                    .filter(workspace -> workspaceId.equals(workspace.workspaceId()))
                    .map(WorkspaceMemberDTO::workspaceName)
                    .findFirst()
                    .orElseThrow(() -> new EntityNotFoundException("Workspace no encontrado: " + workspaceId));
        } catch (ResourceAccessException | ServiceException e) {
            log.warn("No se pudo resolver el nombre del workspace {} contra api-identity: {}",
                    workspaceId, e.getMessage());
            return null;
        }
    }

    public void sendInvitation(Long workspaceId, @Valid WorkspaceSendInvitationDTO body) {
        identityGateway.sendInvitation(workspaceId, body);
    }

    public void acceptRejectInvitation(@Valid AcceptRejectInvitationDTO body) {
        identityGateway.acceptRejectInvitation(body);
    }

    public List<WorkspaceSentInvitationDTO> getSentInvitations() {
        return identityGateway.getSentInvitations();
    }

    public void cancelInvitation(Long invitationId) {
        identityGateway.cancelInvitation(invitationId);
    }
}
