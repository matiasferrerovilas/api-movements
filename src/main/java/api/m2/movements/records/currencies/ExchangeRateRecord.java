package api.m2.movements.records.currencies;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRateRecord(LocalDate date, String base, String quote, BigDecimal rate) {
}
