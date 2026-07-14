package api.m2.movements.records.onboarding;

import java.math.BigDecimal;

public record OnBoardingAmount(BigDecimal amount, String accountToAdd, String bank, String currency) {
}
