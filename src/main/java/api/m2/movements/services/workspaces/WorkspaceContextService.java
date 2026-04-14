package api.m2.movements.services.workspaces;

import api.m2.movements.entities.Workspace;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.repositories.WorkspaceRepository;
import api.m2.movements.services.settings.UserSettingService;
import api.m2.movements.services.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio centralizado para obtener el workspace activo (DEFAULT_WORKSPACE) del usuario autenticado.
 * Este servicio es el punto central para resolver el contexto de workspace en toda la aplicación.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceContextService {

    private final UserSettingService userSettingService;
    private final WorkspaceRepository workspaceRepository;
    private final UserService userService;

    /**
     * Obtiene el workspace activo (DEFAULT_WORKSPACE) del usuario autenticado.
     *
     * @return el Workspace activo del usuario
     * @throws EntityNotFoundException si el usuario no tiene workspace por defecto configurado
     *                                  o si el workspace no existe
     */
    @Transactional(readOnly = true)
    public Workspace getActiveWorkspace() {
        var user = userService.getAuthenticatedUser();
        Long workspaceId = userSettingService.getDefaultWorkspaceId(user)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Usuario sin workspace por defecto configurado"));

        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Workspace no encontrado: " + workspaceId));
    }

    /**
     * Obtiene el ID del workspace activo del usuario autenticado.
     *
     * @return el ID del workspace activo
     * @throws EntityNotFoundException si el usuario no tiene workspace por defecto configurado
     */
    @Transactional(readOnly = true)
    public Long getActiveWorkspaceId() {
        return this.getActiveWorkspace().getId();
    }
}
