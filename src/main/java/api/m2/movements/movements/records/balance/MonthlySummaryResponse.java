package api.m2.movements.movements.records.balance;

import java.util.List;

public record MonthlySummaryResponse(
        Integer year,
        Integer month,
        MonthlySummaryUnifiedRecord totalUnificadoUSD,
        List<MonthlySummaryByCurrencyRecord> porMoneda
) {
}
