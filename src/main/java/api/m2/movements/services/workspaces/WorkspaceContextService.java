package api.m2.movements.services.workspaces;

import api.m2.movements.exceptions.EntityNotFoundException;
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
    private final WorkspaceQueryService workspaceQueryService;

    public Long getActiveWorkspaceId() {
        Long userId = userService.getMe().id();
        return userSettingService.getDefaultWorkspaceId(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Usuario sin workspace por defecto configurado"));
    }

    /**
     * Workspace activo del usuario, validando que pueda escribir en él (rechaza a los miembros
     * READ_ONLY). Para toda operación de alta o modificación disparada desde un request.
     */
    public Long getActiveWritableWorkspaceId() {
        Long workspaceId = this.getActiveWorkspaceId();
        workspaceQueryService.verifyCanWrite(workspaceId);
        return workspaceId;
    }
}
