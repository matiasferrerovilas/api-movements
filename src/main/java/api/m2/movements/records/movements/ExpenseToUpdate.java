package api.m2.movements.records.movements;

import api.m2.movements.enums.BanksEnum;
import api.m2.movements.records.categories.CategoryRecord;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseToUpdate(
        BigDecimal amount,
        LocalDate date,
        String description,
        CategoryRecord category,
        String currency,
        Integer cuotaActual,
        Integer cuotasTotales,
        int year,
        int month,
        BanksEnum bank
) { }