package api.m2.movements.services.workspaces;

import api.m2.movements.entities.Workspace;
import api.m2.movements.exceptions.PermissionDeniedException;
import api.m2.movements.mappers.WorkspaceMapper;
import api.m2.movements.records.workspaces.WorkspaceDetail;
import api.m2.movements.records.workspaces.WorkspaceRecord;
import api.m2.movements.repositories.MembershipRepository;
import api.m2.movements.repositories.WorkspaceRepository;
import api.m2.movements.services.settings.UserSettingService;
import api.m2.movements.services.user.UserService;
import api.m2.movements.exceptions.EntityNotFoundException;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceQueryService {
    private final WorkspaceRepository workspaceRepository;
    private final UserService userService;
    private final WorkspaceMapper workspaceMapper;
    private final MembershipRepository membershipRepository;
    private final UserSettingService userSettingService;

    public List<WorkspaceRecord> findAllWorkspacesOfLogInUser() {
        var owner = userService.getAuthenticatedUser();
        return workspaceRepository.findAllWorkspacesByMemberIdWithAllMembers(owner.getId())
                .stream().map(workspaceMapper::toRecord)
                .toList();
    }

    @Transactional
    public List<WorkspaceDetail> getAllWorkspaceDetails() {
        var owner = userService.getAuthenticatedUser();
        var defaultWorkspaceId = userSettingService.getDefaultWorkspaceId(owner).orElse(null);
        return workspaceRepository.findWorkspaceSummariesByMemberUserId(owner.getId())
                .stream()
                .map(a -> new WorkspaceDetail(
                        a.getAccountId(),
                        a.getAccountName(),
                        a.getMembersCount().intValue(),
                        a.getAccountId().equals(defaultWorkspaceId)))
                .toList();
    }

    public boolean verifyWorkspaceExist(@NotNull String name, Long id) {
        return workspaceRepository.findWorkspaceByNameAndOwnerId(name, id)
                .isPresent();
    }

    public Workspace findWorkspaceByName(String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("El nombre del workspace no puede estar vacío");
        }

        var owner = userService.getAuthenticatedUser();
        return workspaceRepository.findWorkspaceByNameAndOwnerId(name, owner.getId())
                .orElseThrow(() -> new EntityNotFoundException("No existe workspace con ese nombre para el usuario"));
    }

    public Workspace findWorkspaceById(Long workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("No existe workspace con ese id"));
    }

    public void verifyUserIsMemberOfWorkspace(Long workspaceId, Long userId) {
        membershipRepository.findMember(workspaceId, userId)
                .orElseThrow(() -> new PermissionDeniedException("No tienes permiso para operar sobre este recurso"));
    }
}
