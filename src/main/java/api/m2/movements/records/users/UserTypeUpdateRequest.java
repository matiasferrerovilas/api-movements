package api.m2.movements.records.users;

import api.m2.movements.enums.UserType;
import jakarta.validation.constraints.NotNull;

public record UserTypeUpdateRequest(
        @NotNull(message = "El tipo de usuario es obligatorio")
        UserType userType
) { }
