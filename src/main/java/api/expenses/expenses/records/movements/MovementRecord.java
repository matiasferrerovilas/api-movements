package api.expenses.expenses.records.movements;

import api.expenses.expenses.enums.BanksEnum;
import api.expenses.expenses.enums.MovementType;
import api.expenses.expenses.records.categories.CategoryRecord;
import api.expenses.expenses.records.currencies.CurrencyRecord;
import api.expenses.expenses.records.groups.UserGroupsRecord;
import api.expenses.expenses.records.groups.UserRecord;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MovementRecord(Long id,
                             BigDecimal amount,
                             String description,
                             LocalDate date,
                             LocalDateTime createdAt,
                             LocalDateTime updatedAt,
                             CategoryRecord category,
                             CurrencyRecord currency,
                             int year,
                             int month,
                             BanksEnum bank,
                             MovementType type,
                             UserRecord users,
                             UserGroupsRecord userGroups,
                             Integer cuotaActual,
                             Integer cuotasTotales) {
}
