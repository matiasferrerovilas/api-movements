package api.m2.movements.movements.records.pdf;

import java.math.BigDecimal;

public record AmountInfo(BigDecimal pesos, BigDecimal dolares, boolean hasForeignCurrency) { }

