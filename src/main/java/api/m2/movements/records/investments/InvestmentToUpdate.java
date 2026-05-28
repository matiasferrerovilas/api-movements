package api.m2.movements.records.investments;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvestmentToUpdate(
        BigDecimal amount,
        LocalDate startDate,
        LocalDate endDate,
        String description,
        Long investmentTypeId,
        String currencySymbol) {
}
