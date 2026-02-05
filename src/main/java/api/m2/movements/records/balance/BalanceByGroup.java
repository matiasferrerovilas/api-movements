package api.m2.movements.records.balance;

import java.math.BigDecimal;

public record BalanceByGroup(String groupDescription,
                             String currencySymbol,
                             Integer year,
                             Integer month,
                             BigDecimal total) {
}
