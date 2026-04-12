package api.m2.movements.records.workspaces;

import jakarta.validation.constraints.NotBlank;

public record AddWorkspaceRecord(
        @NotBlank(message = "El nombre del workspace es requerido")
        String description) {
}
