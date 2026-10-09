package api.m2.movements.records.pdf;

import api.m2.movements.entities.commons.Currency;
import api.m2.movements.enums.MovementType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Una línea parseada de un extracto bancario. {@code amount} siempre es positivo: el signo del
 * extracto ya quedó traducido en {@code type} (DEBITO/INGRESO en cuenta, CREDITO/REINTEGRO en
 * tarjeta).
 */
public record ParsedExpense(
        LocalDate date,
        String reference,
        Currency currency,
        BigDecimal amount,
        MovementType type
) { }
