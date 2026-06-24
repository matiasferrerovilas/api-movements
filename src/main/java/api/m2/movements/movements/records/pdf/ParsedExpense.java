package api.m2.movements.movements.records.pdf;

import api.m2.movements.movements.entities.commons.Currency;

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
