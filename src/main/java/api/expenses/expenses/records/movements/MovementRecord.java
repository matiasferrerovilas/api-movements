package api.expenses.expenses.records.movements;

import api.expenses.expenses.enums.MovementType;
import api.expenses.expenses.records.accounts.AccountBaseRecord;
import api.expenses.expenses.records.categories.CategoryRecord;
import api.expenses.expenses.records.currencies.CurrencyRecord;
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
                             String bank,
                             MovementType type,
                             UserRecord owner,
                             AccountBaseRecord account,
                             Integer cuotaActual,
                             Integer cuotasTotales) {
}
