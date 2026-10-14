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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class HttpIdentityGateway implements IdentityGateway {

    private final IdentityClient client;

    @Override
    public OnboardingStartResponse startOnboarding(OnboardingStartRequest request) {
        return this.client.startOnboarding(request);
    }

    @Override
    public void changeUserFirstLoginStatus(Long userId) {
        this.client.changeUserFirstLoginStatus(userId);
    }

    @Override
    public List<UserMe> getUsersByIds(List<Long> ids) {
        return this.client.getUsersByIds(ids);
    }

    @Override
    public UserMe getMe(Long workspaceId) {
        return this.client.getMe(workspaceId);
    }

    @Override
    public List<WorkspaceAdded> createWorkspaces(List<AddWorkspaceRecord> workspaces) {
        return this.client.createWorkspaces(workspaces);
    }

    @Override
    public List<WorkspaceMemberDTO> getWorkspaces() {
        return this.client.getWorkspaces();
    }

    @Override
    public List<WorkspaceInvitationDTO> getInvitations() {
        return this.client.getInvitations();
    }

    @Override
    public void verifyMembership(Long workspaceId, Long userId) {
        this.client.verifyMembership(workspaceId, userId);
    }

    @Override
    public void leaveWorkspace(Long workspaceId) {
        this.client.leaveWorkspace(workspaceId);
    }

    @Override
    public void removeMember(Long workspaceId, Long userId) {
        this.client.removeMember(workspaceId, userId);
    }

    @Override
    public void markTourAsSeen() {
        this.client.markTourAsSeen();
    }

    @Override
    public void sendInvitation(Long workspaceId, WorkspaceSendInvitationDTO body) {
        this.client.sendInvitation(workspaceId, body);
    }

    @Override
    public void acceptRejectInvitation(AcceptRejectInvitationDTO body) {
        this.client.acceptRejectInvitation(body);
    }

    @Override
    public List<WorkspaceSentInvitationDTO> getSentInvitations() {
        return this.client.getSentInvitations();
    }

    @Override
    public void cancelInvitation(Long invitationId) {
        this.client.cancelInvitation(invitationId);
    }
}
