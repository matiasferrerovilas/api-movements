package api.m2.movements.records.invite;

import jakarta.validation.constraints.NotNull;

public record InvitationResponseRecord(
        @NotNull(message = "El id de la invitación es requerido")
        Long id,
        boolean status) {
}
