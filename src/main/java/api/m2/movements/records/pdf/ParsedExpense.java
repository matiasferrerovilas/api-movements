package api.m2.movements.records.pdf;

import api.m2.movements.entities.commons.Currency;
import api.m2.movements.enums.MovementType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Una línea parseada de un extracto bancario.
 *
 * <p>{@code amountPesos} es el monto en la moneda local del extracto (ARS para los bancos
 * argentinos, EUR para Santander España) y {@code amountDolares} el monto en moneda extranjera
 * cuando el extracto lo trae aparte. {@code type} es opcional: los extractos de tarjeta dejan
 * {@code null} y la estrategia usa su tipo fijo; los de cuenta lo informan por línea porque mezclan
 * débitos e ingresos.
 */
public record ParsedExpense(
        LocalDate date,
        String reference,
        String installment,
        String comprobante,
        Currency currency,
        BigDecimal amountPesos,
        BigDecimal amountDolares,
        MovementType type
) {
    public ParsedExpense(LocalDate date, String reference, String installment, String comprobante,
                         Currency currency, BigDecimal amountPesos, BigDecimal amountDolares) {
        this(date, reference, installment, comprobante, currency, amountPesos, amountDolares, null);
    }
}
