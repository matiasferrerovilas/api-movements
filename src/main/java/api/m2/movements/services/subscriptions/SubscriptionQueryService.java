package api.m2.movements.services.subscriptions;

import api.m2.movements.entities.movements.Subscription;
import api.m2.movements.mappers.SubscriptionMapper;
import api.m2.movements.records.services.SubscriptionRecord;
import api.m2.movements.repositories.SubscriptionRepository;
import api.m2.movements.services.user.UserService;
import api.m2.movements.services.workspaces.WorkspaceContextService;
import api.m2.movements.services.workspaces.WorkspaceQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriptionQueryService {
    private final SubscriptionMapper subscriptionMapper;
    private final SubscriptionRepository subscriptionRepository;
    private final WorkspaceContextService workspaceContextService;
    private final WorkspaceQueryService workspaceQueryService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<SubscriptionRecord> getSubscriptionsBy(List<String> currencySymbol, LocalDate lastPayment) {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();
        var subscriptions = subscriptionRepository.findByWorkspaceAndCurrencyAndLastPayment(
                workspaceId, currencySymbol, lastPayment);

        return this.toRecords(subscriptions, workspaceId);
    }

    /** Servicios del workspace activo que quedaron sin pagar en un mes puntual — el paso de
     * conciliación del cierre de mes los muestra primero, antes de los números del recap. */
    @Transactional(readOnly = true)
    public List<SubscriptionRecord> getUnpaidForMonth(int year, int month) {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();
        var subscriptions = subscriptionRepository.findUnpaidByWorkspaceAndMonth(workspaceId, year, month);

        return this.toRecords(subscriptions, workspaceId);
    }

    private List<SubscriptionRecord> toRecords(List<Subscription> subscriptions, Long workspaceId) {
        if (subscriptions.isEmpty()) {
            return List.of();
        }

        var workspaceName = workspaceQueryService.findWorkspaceNameById(workspaceId);
        var ownerIds = subscriptions.stream().map(Subscription::getOwnerId).distinct().toList();
        var ownerNamesById = userService.getUserNamesByIds(ownerIds);

        return subscriptions.stream()
                .map(subscription -> subscriptionMapper.toRecord(
                        subscription, workspaceName, ownerNamesById.get(subscription.getOwnerId())))
                .toList();
    }
}
