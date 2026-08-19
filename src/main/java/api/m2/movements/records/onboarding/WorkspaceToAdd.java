package api.m2.movements.records.onboarding;

import jakarta.validation.constraints.NotBlank;

public record WorkspaceToAdd(@NotBlank String name, boolean isDefault) {
}
