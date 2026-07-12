package api.m2.movements.identity.services.workspaces;

import api.m2.movements.clients.IdentityClient;
import api.m2.movements.identity.WorkspaceAdded;
import api.m2.movements.identity.entities.Workspace;
import api.m2.movements.identity.entities.WorkspaceMember;
import api.m2.movements.identity.records.users.UserToAdd;
import api.m2.movements.movements.enums.UserSettingKey;
import api.m2.movements.movements.enums.WorkspaceRole;
import api.m2.movements.exceptions.BusinessException;
import api.m2.movements.exceptions.PermissionDeniedException;
import api.m2.movements.identity.mappers.WorkspaceMapper;
import api.m2.movements.identity.AddWorkspaceRecord;
import api.m2.movements.identity.records.workspaces.WorkspaceDetail;
import api.m2.movements.identity.records.workspaces.WorkspaceLeftEvent;
import api.m2.movements.identity.repositories.MembershipRepository;
import api.m2.movements.identity.repositories.WorkspaceRepository;
import api.m2.movements.identity.services.WorkspacePublishServiceWebSocket;
import api.m2.movements.movements.services.settings.UserSettingService;
import api.m2.movements.identity.services.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceAddService {
    private final WorkspaceQueryService workspaceQueryService;
    private final UserService userService;
    private final WorkspaceRepository workspaceRepository;
    private final MembershipRepository membershipRepository;
    private final WorkspacePublishServiceWebSocket workspacePublishServiceWebSocket;
    private final WorkspaceMapper workspaceMapper;
    private final UserSettingService userSettingService;
    private final ApplicationEventPublisher eventPublisher;

    private final IdentityClient identityClient;

    @Transactional
    public void createWorkspace(AddWorkspaceRecord addWorkspaceRecord) {
        if (StringUtils.isAllBlank(addWorkspaceRecord.description())) {
            throw new BusinessException("La descripción del workspace no puede estar vacía");
        }
        var owner = userService.getAuthenticatedUser();

        var workspace = Workspace.builder()
                .name(addWorkspaceRecord.description())
                .owner(owner)
                .build();

        var existingWorkspace = workspaceQueryService.verifyWorkspaceExist(workspace.getName(), workspace.getOwner().getId());
        if (existingWorkspace) {
            throw new BusinessException("Ya existe un workspace con ese nombre");
        }
        var membership = WorkspaceMember.builder()
                .user(owner)
                .workspace(workspace)
                .role(WorkspaceRole.OWNER)
                .build();
        workspace.getMembers().add(membership);
        workspace = workspaceRepository.save(workspace);

        var keycloakSubject = userService.getCurrentKeycloakId();
        var workspaceDetail = new WorkspaceDetail(workspace.getId(), workspace.getName(), 1, false);
        workspacePublishServiceWebSocket.publishWorkspaceMembershipUpdated(workspaceDetail, keycloakSubject);
    }

    @Transactional
    public void leaveWorkspace(Long workspaceId) {
        var user = userService.getAuthenticatedUser();

        var membership = membershipRepository
                .findMember(workspaceId, user.getId())
                .orElseThrow(() -> new PermissionDeniedException("User does not belong to this workspace"));

        if (membership.getRole() == WorkspaceRole.OWNER) {
            long memberCount = membershipRepository.countByWorkspaceId(workspaceId);
            if (memberCount > 1) {
                throw new PermissionDeniedException("Owner cannot leave the workspace while it has other members");
            }
            var workspace = membership.getWorkspace();
            workspace.setActive(false);
            workspaceRepository.save(workspace);
        }

        membershipRepository.delete(membership);

        if (userSettingService.getDefaultWorkspaceId(user).filter(workspaceId::equals).isPresent()) {
            userSettingService.deleteByKey(UserSettingKey.DEFAULT_WORKSPACE);
        }

        eventPublisher.publishEvent(new WorkspaceLeftEvent(workspaceMapper.toRecord(membership.getWorkspace())));
    }

    @Transactional
    public void addMemberToWorkspace(Workspace workspace) {
        var user = userService.getAuthenticatedUser();
        var membership = WorkspaceMember.builder()
                .user(user)
                .workspace(workspace)
                .role(WorkspaceRole.COLLABORATOR)
                .build();

        workspace.getMembers().add(membership);
        membershipRepository.save(membership);

        long membersCount = membershipRepository.countByWorkspaceId(workspace.getId());
        var workspaceDetail = new WorkspaceDetail(workspace.getId(), workspace.getName(), (int) membersCount, false);
        workspacePublishServiceWebSocket.publishMemberAdded(workspaceDetail, workspace.getId());
    }

    @Transactional
    public void updateDefaultWorkspace(Long id) {

    }

    public List<WorkspaceAdded> createWorkspaces(UserToAdd user, List<AddWorkspaceRecord> workspacesToAdd) {
        return identityClient.createWorkspaces(user.id(), workspacesToAdd);
    }
}
