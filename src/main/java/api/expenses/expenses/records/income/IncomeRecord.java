package api.expenses.expenses.records.income;

import api.expenses.expenses.records.currencies.CurrencyRecord;

import java.math.BigDecimal;

public record IncomeRecord(Long id,
                           BigDecimal amount,
                           CurrencyRecord currency,
                           String bank,
                           String accountName) {
}
