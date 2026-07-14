package api.m2.movements.records.onboarding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OnBoardingForm(OnBoardingAmount onBoardingAmount,
                             @NotBlank String userType,
                             @NotNull List<String> accountsToAdd,
                             @NotNull List<String> categoriesToAdd,
                             @NotNull List<BankToAdd> banksToAdd) {
}
