package api.m2.movements.clients.identity.response;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;

@RegisterReflectionForBinding
public record UserBaseRecord(String givenName, Long id) {
}
