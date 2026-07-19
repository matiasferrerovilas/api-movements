package api.m2.movements.services.workspaces;

import api.m2.movements.clients.identity.IdentityClient;
import api.m2.movements.clients.identity.response.WorkspaceAdded;
import api.m2.movements.enums.UserSettingKey;
import api.m2.movements.exceptions.BusinessException;
import api.m2.movements.clients.identity.requests.AddWorkspaceRecord;
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
    private final UserService userService;
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
        Long userId = userService.getMe().id();

        identityClient.leaveWorkspace(workspaceId);

        userSettingService.getDefaultWorkspaceId(userId)
                .filter(workspaceId::equals)
                .ifPresent(id -> userSettingService.deleteByKey(UserSettingKey.DEFAULT_WORKSPACE));

        log.info("Workspace {} has been removed", workspaceId);
    }

    public List<WorkspaceAdded> createWorkspaces(List<AddWorkspaceRecord> workspacesToAdd) {
        return identityClient.createWorkspaces(workspacesToAdd);
    }
}
