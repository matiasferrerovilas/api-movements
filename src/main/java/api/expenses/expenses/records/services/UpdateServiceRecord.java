package api.expenses.expenses.records.services;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateServiceRecord(BigDecimal amount, String group, LocalDate lastPayment, String description) {
}
