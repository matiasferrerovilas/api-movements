package api.m2.movements.services.workspaces;

import api.m2.movements.clients.identity.IdentityClient;
import api.m2.movements.exceptions.BusinessException;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.exceptions.PermissionDeniedException;
import api.m2.movements.records.workspaces.WorkspaceDTO;
import api.m2.movements.records.workspaces.WorkspaceDetail;
import api.m2.movements.records.workspaces.WorkspaceInvitationDTO;
import api.m2.movements.records.workspaces.WorkspaceMemberDTO;
import api.m2.movements.records.workspaces.WorkspacesWithUser;
import api.m2.movements.services.settings.UserSettingService;
import api.m2.movements.services.user.UserService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceQueryService {
    private final IdentityClient identityClient;
    private final UserService userService;
    private final UserSettingService userSettingService;

    public List<WorkspaceMemberDTO> getWorkspaces() {
        return identityClient.getWorkspaces();
    }

    public Long findWorkspaceIdByName(@NotNull String name) {
        /*if (StringUtils.isBlank(name)) {
            throw new BusinessException("El nombre del workspace no puede estar vacío");
        }

        Long userId = userService.getMe().id();


        return identityClient.getWorkspaces().stream()
                .filter(workspace -> name.equals(workspace.name()))
                .map(WorkspacesWithUser::id)
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("
                No existe workspace con ese nombre para el usuario"));
         */
        return null;
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
}
