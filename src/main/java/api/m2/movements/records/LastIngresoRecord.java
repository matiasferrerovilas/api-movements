package api.m2.movements.records;

import api.m2.movements.enums.BanksEnum;
import api.m2.movements.records.currencies.CurrencyRecord;

import java.math.BigDecimal;

public record LastIngresoRecord(Long id,
                                BigDecimal amount,
                                String description,
                                CurrencyRecord currency,
                                String group,
                                BanksEnum bank) {
}
