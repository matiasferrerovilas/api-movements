package api.m2.movements.clients.identity;

import api.m2.movements.records.workspaces.AddWorkspaceRecord;
import api.m2.movements.records.workspaces.WorkspaceAdded;
import api.m2.movements.records.users.UserBaseRecord;
import api.m2.movements.records.workspaces.WorkspaceDTO;
import api.m2.movements.records.workspaces.WorkspaceInvitationDTO;
import api.m2.movements.records.workspaces.WorkspaceMemberDTO;
import api.m2.movements.records.workspaces.WorkspacesWithUser;
import api.m2.movements.clients.identity.requests.UserToAdd;
import api.m2.movements.clients.identity.response.UserMe;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

import java.util.List;

@HttpExchange
public interface IdentityClient {

    @PostExchange("/v1/users")
    UserMe createLogInUser(@RequestBody UserToAdd user);

    @PatchExchange("/v1/onboarding/{userId}/first-login")
    void changeUserFirstLoginStatus(@PathVariable Long userId);

    @GetExchange("/v1/users/by-email")
    UserBaseRecord getUserByEmail(@RequestParam String email);

    @GetExchange("/v1/users")
    List<UserMe> getUsersByIds(@RequestParam List<Long> ids);

    @GetExchange("/v1/users/me")
    UserMe getMe();

    @PostExchange("/v1/workspaces")
    List<WorkspaceAdded> createWorkspaces(@RequestBody List<AddWorkspaceRecord> workspaces);

    @GetExchange("/v1/workspaces/members")
    List<WorkspaceMemberDTO> getWorkspaces();

    @GetExchange("/v1/workspaces/{workspaceId}")
    WorkspaceDTO getWorkspaceById(@PathVariable Long workspaceId);

    @GetExchange("/v1/workspaces/invitations")
    List<WorkspaceInvitationDTO> getInvitations();

    @GetExchange("/v1/workspaces/{workspaceId}/members/{userId}")
    void verifyMembership(@PathVariable Long workspaceId, @PathVariable Long userId);

    @DeleteExchange("/v1/workspaces/{workspaceId}/members/{userId}")
    void leaveWorkspace(@PathVariable Long workspaceId, @PathVariable Long userId);

    @PutExchange("/v1/onboarding/tour")
    void markTourAsSeen();
}
