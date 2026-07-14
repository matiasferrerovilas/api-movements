package api.m2.movements.services.workspaces;

import api.m2.movements.clients.identity.IdentityClient;
import api.m2.movements.clients.identity.response.UserMe;
import api.m2.movements.records.workspaces.WorkspaceAdded;
import api.m2.movements.clients.identity.requests.UserToAdd;
import api.m2.movements.enums.UserSettingKey;
import api.m2.movements.exceptions.BusinessException;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.records.workspaces.AddWorkspaceRecord;
import api.m2.movements.services.WorkspacePublishServiceWebSocket;
import api.m2.movements.services.settings.UserSettingService;
import api.m2.movements.services.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceAddService {
    private final WorkspaceQueryService workspaceQueryService;
    private final UserService userService;
    private final WorkspacePublishServiceWebSocket workspacePublishServiceWebSocket;
    private final UserSettingService userSettingService;
    private final IdentityClient identityClient;

    @Transactional
    public void createWorkspace(AddWorkspaceRecord addWorkspaceRecord) {
        if (StringUtils.isAllBlank(addWorkspaceRecord.description())) {
            throw new BusinessException("La descripción del workspace no puede estar vacía");
        }
        identityClient.createWorkspaces(List.of(addWorkspaceRecord));
    }

    @Transactional
    public void leaveWorkspace(Long workspaceId) {
        Long userId = userService.getAuthenticatedUser().id();

        identityClient.leaveWorkspace(workspaceId, userId);

        userSettingService.getDefaultWorkspaceId(userId)
                .filter(workspaceId::equals)
                .ifPresent(id -> userSettingService.deleteByKey(UserSettingKey.DEFAULT_WORKSPACE));
    }

    @Transactional
    public void updateDefaultWorkspace(Long workspaceId) {
        Long userId = userService.getAuthenticatedUser().id();
        userSettingService.upsertForUser(userId, UserSettingKey.DEFAULT_WORKSPACE, workspaceId);

        var workspaceDetail = workspaceQueryService.getAllWorkspaceDetails().stream()
                .filter(detail -> detail.id().equals(workspaceId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Workspace no encontrado: " + workspaceId));

        var keycloakSubject = userService.getCurrentKeycloakId();
        workspacePublishServiceWebSocket.publishWorkspaceMembershipUpdated(workspaceDetail, keycloakSubject);
    }

    public List<WorkspaceAdded> createWorkspaces(List<AddWorkspaceRecord> workspacesToAdd) {
        return identityClient.createWorkspaces(workspacesToAdd);
    }
}
