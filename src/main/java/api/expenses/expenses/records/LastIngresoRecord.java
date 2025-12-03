package api.expenses.expenses.records;

import api.expenses.expenses.enums.BanksEnum;
import api.expenses.expenses.records.currencies.CurrencyRecord;

import java.math.BigDecimal;

public record LastIngresoRecord(Long id,
                                BigDecimal amount,
                                String description,
                                CurrencyRecord currency,
                                String group,
                                BanksEnum bank) {
}
