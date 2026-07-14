package api.m2.movements.records.services;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateSubscriptionRecord(BigDecimal amount, String workspace, LocalDate lastPayment, String description) {
}

