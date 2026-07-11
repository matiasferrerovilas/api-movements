package api.m2.movements.identity.records.users;

import api.m2.movements.movements.enums.UserType;
import lombok.Builder;

@Builder
public record UserToAdd(String email, String givenName, String familyName, boolean isFirstLogin, UserType userType) {
}
