package api.m2.movements.records.balance;

import java.math.BigDecimal;

public record RecoveryTimeRecord(
        BigDecimal monto,
        String moneda,
        Integer mesesConsiderados,
        BigDecimal ahorroPromedioMensual,
        BigDecimal mesesParaRecuperar,
        boolean recuperable
) {
}
