package api.m2.movements.clients.identity.requests;

import api.m2.movements.enums.UserType;
import lombok.Builder;

@Builder
public record UserToAdd(Long id, String email, String givenName, String familyName, boolean isFirstLogin, UserType userType) {
}
