package api.m2.movements.records.movements;

import api.m2.movements.records.categories.CategoryRecord;
import api.m2.movements.records.currencies.CurrencyRecord;
import api.m2.movements.clients.identity.response.UserBaseRecord;
import api.m2.movements.records.workspaces.WorkspaceBaseRecord;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MovementRecord(Long id,
                             BigDecimal amount,
                             String description,
                             LocalDate date,
                             LocalDateTime createdAt,
                             LocalDateTime updatedAt,
                             List<CategoryRecord> categories,
                             CurrencyRecord currency,
                             String bank,
                             String type,
                             Integer cuotaActual,
                             Integer cuotasTotales,
                             LocalDate lastCreditPayment,
                             Metadata metadata,
                             // Desglose opcional del movimiento. Nunca null: [] si no tiene items.
                             List<MovementItemDto> items) {

    public record Metadata(UserBaseRecord owner,
                            WorkspaceBaseRecord workspace,
                            BigDecimal exchangeRate,
                            BigDecimal amountUsd) {
    }
}
