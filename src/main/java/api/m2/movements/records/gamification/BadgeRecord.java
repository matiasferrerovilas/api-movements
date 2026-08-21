package api.m2.movements.records.gamification;

import api.m2.movements.enums.BadgeType;

import java.time.LocalDateTime;

public record BadgeRecord(
        Long id,
        String categoryDescription,
        BadgeType type,
        int year,
        int month,
        LocalDateTime earnedAt) {
}
