package api.m2.movements.services.movements;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.ZoneOffset;

/**
 * Genera la próxima cuota de cada compra en cuotas (CREDITO) que todavía no llegó a
 * cuotasTotales — sin esto, una compra manual en cuotas solo aparecía como gasto el mes en que
 * se cargó, aunque el resto del plan siguiera cobrándose mes a mes (antes esto lo resolvía
 * reimportar el resumen del banco todos los meses, canal que hoy está desactivado en el frontend).
 *
 * Se ejecuta el primer día de cada mes a las 6:30 AM (mismo horario que {@code RecurringIncomeJob},
 * corrido media hora para no competir con él).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CreditInstallmentJob {

    private final MovementAddService movementAddService;

    @Scheduled(cron = "0 30 6 1 * *")
    public void generateNextInstallments() {
        var previousMonth = YearMonth.now(ZoneOffset.UTC).minusMonths(1);
        int count = movementAddService.generateNextCreditInstallments(previousMonth);
        log.info("Cuotas de crédito generadas: {} (a partir de movimientos de {})", count, previousMonth);
    }
}
