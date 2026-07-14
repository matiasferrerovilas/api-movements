package api.m2.movements.records.onboarding;

import jakarta.validation.constraints.NotBlank;

public record BankToAdd(@NotBlank String description, boolean isDefault) {
}
