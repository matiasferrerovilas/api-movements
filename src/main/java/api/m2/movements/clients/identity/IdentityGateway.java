package api.m2.movements.clients.identity;

import api.m2.movements.clients.identity.requests.AcceptRejectInvitationDTO;
import api.m2.movements.clients.identity.requests.AddWorkspaceRecord;
import api.m2.movements.clients.identity.requests.OnboardingStartRequest;
import api.m2.movements.clients.identity.requests.WorkspaceSendInvitationDTO;
import api.m2.movements.clients.identity.response.OnboardingStartResponse;
import api.m2.movements.clients.identity.response.UserMe;
import api.m2.movements.clients.identity.response.WorkspaceAdded;
import api.m2.movements.clients.identity.response.WorkspaceInvitationDTO;
import api.m2.movements.clients.identity.response.WorkspaceMemberDTO;
import api.m2.movements.clients.identity.response.WorkspaceSentInvitationDTO;

import java.util.List;

/**
 * Everything api-movements needs from an identity backend (users, workspaces, memberships, invitations).
 * Implement this interface to plug in a different identity service; the default adapter is
 * {@code HttpIdentityGateway}, which talks to api-identity over HTTP.
 */
public interface IdentityGateway {

    OnboardingStartResponse startOnboarding(OnboardingStartRequest request);

    void changeUserFirstLoginStatus(Long userId);

    List<UserMe> getUsersByIds(List<Long> ids);

    UserMe getMe(Long workspaceId);

    List<WorkspaceAdded> createWorkspaces(List<AddWorkspaceRecord> workspaces);

    List<WorkspaceMemberDTO> getWorkspaces();

    List<WorkspaceInvitationDTO> getInvitations();

    void verifyMembership(Long workspaceId, Long userId);

    void leaveWorkspace(Long workspaceId);

    void removeMember(Long workspaceId, Long userId);

    void markTourAsSeen();

    void sendInvitation(Long workspaceId, WorkspaceSendInvitationDTO body);

    void acceptRejectInvitation(AcceptRejectInvitationDTO body);

    List<WorkspaceSentInvitationDTO> getSentInvitations();

    void cancelInvitation(Long invitationId);
}
