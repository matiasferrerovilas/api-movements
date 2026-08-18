package api.m2.movements.records.balance;

import java.math.BigDecimal;

public record CategoryAmountRecord(String category, BigDecimal amount) {
}
