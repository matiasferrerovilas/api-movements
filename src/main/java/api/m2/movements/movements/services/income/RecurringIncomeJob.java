package api.m2.movements.movements.services.income;

import api.m2.movements.movements.services.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Job que genera movimientos de ingreso automáticamente para usuarios
 * que tienen habilitado AUTO_INCOME_ENABLED.
 *
 * Se ejecuta el primer día de cada mes a las 6:00 AM.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RecurringIncomeJob {

    private final UserService userService;
    private final IncomeAddService incomeAddService;

    @Scheduled(cron = "0 0 6 1 * *")
    public void generateRecurringIncomes() {
        List<Long> userIds = userService.getUsersWithAutoIncomeEnabled();
        log.info("Generando ingresos recurrentes para {} usuarios", userIds.size());

        int totalMovements = 0;
        for (Long userId : userIds) {
            try {
                int count = incomeAddService.generateRecurringIncomeForUser(userId);
                totalMovements += count;
            } catch (Exception e) {
                log.error("Error generando ingresos para usuario {}: {}",
                        userId, e.getMessage(), e);
            }
        }

        log.info("Ingresos recurrentes generados: {} movimientos para {} usuarios",
                totalMovements, userIds.size());
    }
}
