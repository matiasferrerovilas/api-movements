package api.m2.movements.investment.records;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvestmentToUpdate(
        BigDecimal amount,
        LocalDate startDate,
        LocalDate endDate,
        String description,
        String symbol,
        BigDecimal tna,
        Long investmentTypeId,
        String currencySymbol) {
}
