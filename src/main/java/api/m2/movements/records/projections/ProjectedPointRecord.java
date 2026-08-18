package api.m2.movements.records.projections;

import java.math.BigDecimal;

public record ProjectedPointRecord(Integer monthsOut, BigDecimal projectedBalance) {
}
