package api.m2.movements.records.goals;

import api.m2.movements.records.currencies.CurrencyRecord;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record GoalRecord(
        Long id,
        Long workspaceId,
        String name,
        BigDecimal targetAmount,
        BigDecimal currentAmount,
        CurrencyRecord currency,
        LocalDate targetDate,
        BigDecimal progressPercent,
        LocalDateTime createdAt
) {
}
