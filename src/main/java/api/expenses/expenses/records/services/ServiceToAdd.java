package api.expenses.expenses.records.services;

import api.expenses.expenses.records.currencies.CurrencyRecord;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ServiceToAdd(String description, BigDecimal amount,
                           CurrencyRecord currency, LocalDate lastPayment,
                           Boolean isPaid,
                           Long accountId) {
}
