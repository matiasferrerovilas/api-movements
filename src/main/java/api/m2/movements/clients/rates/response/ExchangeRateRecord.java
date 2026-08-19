package api.m2.movements.clients.rates.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRateRecord(LocalDate date, String base, String quote, BigDecimal rate) {
}
