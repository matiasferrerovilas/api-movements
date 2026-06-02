package api.m2.movements.investment.records;

import api.m2.movements.investment.enums.InvestmentCategory;

import java.math.BigDecimal;

public record InvestmentValuationRecord(
        Long investmentId,
        String description,
        BigDecimal investedAmount,
        BigDecimal currentValue,
        BigDecimal currentPrice,
        String symbol,
        InvestmentCategory category,
        String currencySymbol) {
}
