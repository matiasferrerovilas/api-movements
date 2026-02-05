package api.m2.movements.records.pdf;

import api.m2.movements.entities.Currency;

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
) { }
