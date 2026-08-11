package api.m2.movements.records.balance;

import java.math.BigDecimal;

public record BalanceByCategoryRecord(String category,
                                      Long year,
                                      Long month,
                                      String currencySymbol,
                                      BigDecimal total) {
}
