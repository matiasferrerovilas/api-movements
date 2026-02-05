package api.m2.movements.records.movements;

import api.m2.movements.records.accounts.AccountBaseRecord;
import api.m2.movements.records.categories.CategoryRecord;
import api.m2.movements.records.currencies.CurrencyRecord;
import api.m2.movements.records.groups.UserRecord;

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
                             String type,
                             UserRecord owner,
                             AccountBaseRecord account,
                             Integer cuotaActual,
                             Integer cuotasTotales) {
}
