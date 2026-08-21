package api.m2.movements.records.gamification;

import java.time.LocalDate;

public record StreakRecord(
        int currentStreak,
        int longestStreak,
        LocalDate lastActivityDate) {
}
