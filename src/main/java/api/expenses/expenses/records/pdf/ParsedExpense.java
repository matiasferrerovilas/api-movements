package api.expenses.expenses.records.pdf;

import api.expenses.expenses.entities.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParsedExpense(
        LocalDate date,
        String reference,
        String installment,
        String comprobante,
        Currency currency,
        BigDecimal amountPesos,
        BigDecimal amountDolares
) {}
