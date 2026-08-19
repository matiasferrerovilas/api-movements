package api.m2.movements.records.onboarding;

import api.m2.movements.records.currencies.CurrencyToAdd;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OnBoardingForm(OnBoardingAmount onBoardingAmount,
                             @NotBlank String userType,
                             @NotNull List<WorkspaceToAdd> workspacesToAdd,
                             @NotNull List<String> categoriesToAdd,
                             @NotNull List<BankToAdd> banksToAdd,
                             @NotNull List<CurrencyToAdd> currenciesToAdd) {
}
