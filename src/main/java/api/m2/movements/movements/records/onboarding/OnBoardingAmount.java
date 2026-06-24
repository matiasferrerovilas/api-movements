package api.m2.movements.movements.records.onboarding;

import java.math.BigDecimal;

public record OnBoardingAmount(BigDecimal amount, String accountToAdd, String bank, String currency) {
}
