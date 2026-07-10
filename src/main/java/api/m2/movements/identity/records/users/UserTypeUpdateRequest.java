package api.m2.movements.identity.records.users;

import api.m2.movements.movements.enums.UserType;
import jakarta.validation.constraints.NotNull;

public record UserTypeUpdateRequest(
        @NotNull(message = "El tipo de usuario es obligatorio")
        UserType userType
) { }
