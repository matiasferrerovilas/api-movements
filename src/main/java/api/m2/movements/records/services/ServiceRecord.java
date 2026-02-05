package api.m2.movements.records.services;

import api.m2.movements.records.currencies.CurrencyRecord;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ServiceRecord(Long id, String description, BigDecimal amount,
                            CurrencyRecord currency,
                            LocalDate lastPayment,
                            Boolean isPaid,
                            String group,
                            String user) {
}
