package api.m2.movements.clients;

import api.m2.movements.identity.records.users.UserToAdd;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface IdentityClient {

    @PostExchange("/v1/users")
    UserToAdd createLogInUser(@RequestBody UserToAdd user);

    @PatchExchange("/v1/users/{userId}/first-login")
    void changeUserFirstLoginStatus(@PathVariable Long userId);
}
