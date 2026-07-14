package api.m2.movements.records.income;

import api.m2.movements.records.currencies.CurrencyRecord;

import java.math.BigDecimal;

public record IncomeRecord(Long id,
                           BigDecimal amount,
                           CurrencyRecord currency,
                           String bank,
                           String accountName) {
}
