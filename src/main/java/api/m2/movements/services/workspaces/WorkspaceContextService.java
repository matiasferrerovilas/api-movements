package api.m2.movements.services.workspaces;

import api.m2.movements.clients.identity.IdentityClient;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.clients.identity.response.WorkspaceMemberDTO;
import api.m2.movements.services.settings.UserSettingService;
import api.m2.movements.services.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceContextService {

    private final UserSettingService userSettingService;
    private final UserService userService;
    private final IdentityClient identityClient;

    public Long getActiveWorkspaceId() {
        Long userId = userService.getMe().id();
        return userSettingService.getDefaultWorkspaceId(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Usuario sin workspace por defecto configurado"));
    }

    public WorkspaceMemberDTO getActiveWorkspace() {
        Long workspaceId = this.getActiveWorkspaceId();
        return identityClient.getWorkspaces().stream()
                .filter(workspace -> workspaceId.equals(workspace.workspaceId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Workspace activo no encontrado: " + workspaceId));
    }
}
