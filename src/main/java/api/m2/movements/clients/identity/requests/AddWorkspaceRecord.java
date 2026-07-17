package api.m2.movements.clients.identity.requests;

import jakarta.validation.constraints.NotBlank;

public record AddWorkspaceRecord(
        @NotBlank(message = "El nombre del workspace es requerido")
        String description) {
}
