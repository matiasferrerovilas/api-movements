package api.m2.movements.records.movements;

import api.m2.movements.records.categories.CategoryRecord;
import api.m2.movements.records.currencies.CurrencyRecord;
import api.m2.movements.records.users.UserBaseRecord;
import api.m2.movements.records.workspaces.WorkspaceBaseRecord;

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
                             Integer cuotaActual,
                             Integer cuotasTotales,
                             Metadata metadata) {

    public record Metadata(UserBaseRecord owner,
                            WorkspaceBaseRecord workspace,
                            BigDecimal exchangeRate,
                            BigDecimal amountUsd) {
    }
}
