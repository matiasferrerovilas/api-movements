package api.m2.movements.identity.services.workspaces;

import api.m2.movements.clients.IdentityClient;
import api.m2.movements.exceptions.BusinessException;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.exceptions.PermissionDeniedException;
import api.m2.movements.identity.records.workspaces.WorkspaceDetail;
import api.m2.movements.movements.services.settings.UserSettingService;
import api.m2.movements.movements.services.user.UserService;
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

    public List<WorkspaceDetail> getAllWorkspaceDetails() {
        Long userId = userService.getAuthenticatedUser().id();
        Long defaultWorkspaceId = userSettingService.getDefaultWorkspaceId(userId).orElse(null);
        return identityClient.getWorkspaces(userId).stream()
                .map(workspace -> new WorkspaceDetail(
                        workspace.id(),
                        workspace.name(),
                        (int) workspace.membersCount(),
                        workspace.id().equals(defaultWorkspaceId)))
                .toList();
    }

    public Long findWorkspaceIdByName(@NotNull String name) {
        if (StringUtils.isBlank(name)) {
            throw new BusinessException("El nombre del workspace no puede estar vacío");
        }

        Long userId = userService.getAuthenticatedUser().id();
        return identityClient.getWorkspaces(userId).stream()
                .filter(workspace -> name.equals(workspace.name()))
                .map(workspace -> workspace.id())
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("No existe workspace con ese nombre para el usuario"));
    }

    public void verifyUserIsMemberOfWorkspace(Long workspaceId, Long userId) {
        try {
            identityClient.verifyMembership(workspaceId, userId);
        } catch (RestClientResponseException e) {
            throw new PermissionDeniedException("No tienes permiso para operar sobre este recurso");
        }
    }
}
