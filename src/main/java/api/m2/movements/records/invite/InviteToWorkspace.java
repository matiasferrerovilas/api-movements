package api.m2.movements.records.invite;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record InviteToWorkspace(
        @NotEmpty(message = "Debe incluir al menos un email")
        List<String> emails) {
}
