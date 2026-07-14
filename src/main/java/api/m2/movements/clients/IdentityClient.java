package api.m2.movements.clients;

import api.m2.movements.movements.records.workspaces.AddWorkspaceRecord;
import api.m2.movements.movements.records.workspaces.WorkspaceAdded;
import api.m2.movements.identity.records.users.UserBaseRecord;
import api.m2.movements.identity.records.workspaces.WorkspacesWithUser;
import api.m2.movements.identity.records.users.UserToAdd;
import api.m2.movements.movements.records.users.UserMe;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

@HttpExchange
public interface IdentityClient {

    @PostExchange("/v1/users")
    UserToAdd createLogInUser(@RequestBody UserToAdd user);

    @PatchExchange("/v1/users/{userId}/first-login")
    void changeUserFirstLoginStatus(@PathVariable Long userId);

    @GetExchange("/v1/users/by-email")
    UserBaseRecord getUserByEmail(@RequestParam String email);

    @GetExchange("/v1/users/me")
    UserMe getMe(@RequestHeader("Authorization") String authorization);

    @PostExchange("/v1/users/{userId}/workspaces")
    List<WorkspaceAdded> createWorkspaces(@PathVariable Long userId, @RequestBody List<AddWorkspaceRecord> workspaces);

    @GetExchange("/v1/users/{userId}/workspaces")
    List<WorkspacesWithUser> getWorkspaces(@PathVariable Long userId);

    @GetExchange("/v1/workspaces/{workspaceId}/members/{userId}")
    void verifyMembership(@PathVariable Long workspaceId, @PathVariable Long userId);

    @DeleteExchange("/v1/workspaces/{workspaceId}/members/{userId}")
    void leaveWorkspace(@PathVariable Long workspaceId, @PathVariable Long userId);
}
