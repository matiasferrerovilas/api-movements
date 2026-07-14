package api.m2.movements.identity.services.workspaces;

import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.movements.services.settings.UserSettingService;
import api.m2.movements.movements.services.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Servicio centralizado para obtener el workspace activo (DEFAULT_WORKSPACE) del usuario autenticado.
 * Este servicio es el punto central para resolver el contexto de workspace en toda la aplicación.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceContextService {

    private final UserSettingService userSettingService;
    private final UserService userService;

    /**
     * Obtiene el ID del workspace activo del usuario autenticado.
     *
     * @return el ID del workspace activo
     * @throws EntityNotFoundException si el usuario no tiene workspace por defecto configurado
     */
    public Long getActiveWorkspaceId() {
        Long userId = userService.getAuthenticatedUser().id();
        return userSettingService.getDefaultWorkspaceId(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Usuario sin workspace por defecto configurado"));
    }
}
