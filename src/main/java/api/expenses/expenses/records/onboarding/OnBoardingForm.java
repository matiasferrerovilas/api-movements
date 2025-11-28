package api.expenses.expenses.records.onboarding;

import java.util.List;

public record OnBoardingForm(OnBoardingAmount onBoardingAmount,
                             String bank,
                             String currency,
                             List<String> groups) {
}
