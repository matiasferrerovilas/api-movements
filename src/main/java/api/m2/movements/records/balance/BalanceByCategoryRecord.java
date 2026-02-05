package api.m2.movements.records.balance;

import java.math.BigDecimal;

public record BalanceByCategoryRecord(String category,
                                      Integer year,
                                      Integer month,
                                      String currencySymbol,
                                      BigDecimal total) {
}
