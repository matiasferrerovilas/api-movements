package api.m2.movements.investment.records;

import api.m2.movements.records.currencies.CurrencyRecord;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvestmentRecord(
        Long id,
        BigDecimal amount,
        LocalDate startDate,
        LocalDate endDate,
        String description,
        String symbol,
        BigDecimal tna,
        InvestmentTypeRecord investmentType,
        CurrencyRecord currency,
        String workspaceName,
        Long workspaceId,
        String owner) {

    @JsonIgnore
    @Override
    public Long workspaceId() {
        return workspaceId;
    }
}
