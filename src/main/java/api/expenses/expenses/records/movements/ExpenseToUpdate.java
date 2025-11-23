package api.expenses.expenses.records.movements;

import api.expenses.expenses.enums.BanksEnum;
import api.expenses.expenses.records.categories.CategoryRecord;

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