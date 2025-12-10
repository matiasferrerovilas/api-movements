package api.expenses.expenses.records.income;

import api.expenses.expenses.records.currencies.CurrencyRecord;
import api.expenses.expenses.records.groups.UserGroupsRecord;

import java.math.BigDecimal;

public record IncomeRecord(Long id,
                           BigDecimal amount,
                           CurrencyRecord currency,
                           String bank,
                           UserGroupsRecord groups) {
}
