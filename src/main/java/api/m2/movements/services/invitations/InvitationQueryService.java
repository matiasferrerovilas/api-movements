package api.m2.movements.services.invitations;

import api.m2.movements.enums.InvitationStatus;
import api.m2.movements.mappers.WorkspaceInvitationMapper;
import api.m2.movements.records.invite.InvitationToWorkspaceRecord;
import api.m2.movements.repositories.WorkspaceInvitationRepository;
import api.m2.movements.services.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationQueryService {

    private final UserService userService;
    private final WorkspaceInvitationRepository workspaceInvitationRepository;
    private final WorkspaceInvitationMapper workspaceInvitationMapper;

    public List<InvitationToWorkspaceRecord> getAllInvitations() {
        var user = userService.getAuthenticatedUserRecord();
        return workspaceInvitationMapper.toRecord(
                workspaceInvitationRepository.findAllByUserIdAndStatus(user.id(), InvitationStatus.PENDING));
    }
}
