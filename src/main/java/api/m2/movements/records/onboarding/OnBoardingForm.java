package api.m2.movements.records.onboarding;

import java.util.List;

public record OnBoardingForm(OnBoardingAmount onBoardingAmount,
                             String userType,
                             List<String> accountsToAdd) {
}
