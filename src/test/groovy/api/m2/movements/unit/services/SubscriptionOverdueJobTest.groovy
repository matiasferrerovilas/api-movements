package api.m2.movements.unit.services

import api.m2.movements.entities.movements.Subscription
import api.m2.movements.enums.NotificationSeverity
import api.m2.movements.repositories.SubscriptionRepository
import api.m2.movements.services.notifications.NotificationService
import api.m2.movements.services.subscriptions.SubscriptionOverdueJob
import spock.lang.Requires
import spock.lang.Specification

import java.time.LocalDate

/**
 * Nota: la ejecucion de este job depende de la fecha real (dia >= 3 del mes),
 * igual que RecurringIncomeJob/MonthlySummaryJob no abstraen el reloj. Los tests
 * que dependen de estar dentro del rango de dias se saltan fuera de ese rango.
 */
class SubscriptionOverdueJobTest extends Specification {

    SubscriptionRepository subscriptionRepository = Mock(SubscriptionRepository)
    NotificationService notificationService = Mock(NotificationService)

    SubscriptionOverdueJob job

    def setup() {
        job = new SubscriptionOverdueJob(subscriptionRepository, notificationService)
    }

    @Requires({ LocalDate.now().dayOfMonth < 3 })
    def "notifyOverdueSubscriptions - should not query when it is still within the grace period"() {
        when:
        job.notifyOverdueSubscriptions()

        then:
        0 * subscriptionRepository.findOverdueUnpaidAndUnnotified(_ as int, _ as int)
    }

    @Requires({ LocalDate.now().dayOfMonth >= 3 })
    def "notifyOverdueSubscriptions - should publish an ERROR notification and mark the subscription notified"() {
        given:
        def today = LocalDate.now()
        def subscription = new Subscription(id: 1L, description: "Alquiler", workspaceId: 5L)
        subscriptionRepository.findOverdueUnpaidAndUnnotified(today.year, today.monthValue) >> [subscription]

        when:
        job.notifyOverdueSubscriptions()

        then:
        1 * notificationService.publish(5L, "Alquiler sin pagar",
                "Alquiler — vence hace ${today.dayOfMonth} días", NotificationSeverity.ERROR)
        1 * subscriptionRepository.save({ Subscription s ->
            s.overdueNotifiedYear == today.year && s.overdueNotifiedMonth == today.monthValue
        })
    }

    @Requires({ LocalDate.now().dayOfMonth >= 3 })
    def "notifyOverdueSubscriptions - should not notify when there are no candidates"() {
        given:
        def today = LocalDate.now()
        subscriptionRepository.findOverdueUnpaidAndUnnotified(today.year, today.monthValue) >> []

        when:
        job.notifyOverdueSubscriptions()

        then:
        0 * notificationService.publish(_ as Long, _ as String, _ as String, _ as NotificationSeverity)
        0 * subscriptionRepository.save(_ as Subscription)
    }
}
