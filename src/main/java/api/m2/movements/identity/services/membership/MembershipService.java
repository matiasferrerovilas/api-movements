package api.m2.movements.identity.services.membership;

import api.m2.movements.identity.projections.MembershipSummaryProjection;
import api.m2.movements.identity.repositories.MembershipRepository;
import api.m2.movements.identity.services.user.UserService;
import api.m2.movements.identity.services.workspaces.WorkspaceContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class MembershipService {
    private final MembershipRepository membershipRepository;
    private final UserService userService;
    private final WorkspaceContextService workspaceContextService;

    @Transactional(readOnly = true)
    public List<MembershipSummaryProjection> getAllMemberships() {
        var user = userService.getAuthenticatedUserRecord();
        return membershipRepository.findAllByUserId(user.id());
    }

    @Transactional(readOnly = true)
    public List<String> getMemberEmails() {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();
        return membershipRepository.findMemberEmailsByWorkspaceId(workspaceId);
    }
}