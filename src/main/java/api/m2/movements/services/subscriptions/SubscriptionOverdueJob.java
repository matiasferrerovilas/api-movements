package api.m2.movements.services.subscriptions;

import api.m2.movements.enums.NotificationSeverity;
import api.m2.movements.repositories.SubscriptionRepository;
import api.m2.movements.services.notifications.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Notifica servicios/suscripciones sin pagar a partir del tercer día del mes.
 * El campo overdueNotifiedYear/Month en Subscription evita reenviar la misma
 * notificación todos los días mientras el servicio siga impago en el período.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SubscriptionOverdueJob {

    private static final int GRACE_PERIOD_DAYS = 3;

    private final SubscriptionRepository subscriptionRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void notifyOverdueSubscriptions() {
        var today = LocalDate.now(ZoneOffset.UTC);
        if (today.getDayOfMonth() < GRACE_PERIOD_DAYS) {
            return;
        }

        var year = today.getYear();
        var month = today.getMonthValue();
        var overdueSubscriptions = subscriptionRepository.findOverdueUnpaidAndUnnotified(year, month);
        log.info("Suscripciones vencidas sin notificar: {}", overdueSubscriptions.size());

        overdueSubscriptions.forEach(subscription -> {
            notificationService.publish(subscription.getWorkspaceId(), "Alquiler sin pagar",
                    subscription.getDescription() + " — vence hace " + today.getDayOfMonth() + " días",
                    NotificationSeverity.ERROR);
            subscription.setOverdueNotifiedYear(year);
            subscription.setOverdueNotifiedMonth(month);
            subscriptionRepository.save(subscription);
        });
    }
}
