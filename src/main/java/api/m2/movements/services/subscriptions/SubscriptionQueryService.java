package api.m2.movements.services.subscriptions;

import api.m2.movements.entities.movements.Subscription;
import api.m2.movements.mappers.SubscriptionMapper;
import api.m2.movements.records.services.SubscriptionRecord;
import api.m2.movements.repositories.SubscriptionRepository;
import api.m2.movements.services.user.UserService;
import api.m2.movements.services.workspaces.WorkspaceContextService;
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
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<SubscriptionRecord> getSubscriptionsBy(List<String> currencySymbol, LocalDate lastPayment) {
        var activeWorkspace = workspaceContextService.getActiveWorkspace();
        var subscriptions = subscriptionRepository.findByWorkspaceAndCurrencyAndLastPayment(
                activeWorkspace.workspaceId(), currencySymbol, lastPayment);

        if (subscriptions.isEmpty()) {
            return List.of();
        }

        var ownerIds = subscriptions.stream().map(Subscription::getOwnerId).distinct().toList();
        var ownerNamesById = userService.getUserNamesByIds(ownerIds);

        return subscriptions.stream()
                .map(subscription -> this.enrich(
                        subscriptionMapper.toRecord(subscription), activeWorkspace.workspaceName(),
                        ownerNamesById.get(subscription.getOwnerId())))
                .toList();
    }

    private SubscriptionRecord enrich(SubscriptionRecord record, String workspaceName, String ownerName) {
        return new SubscriptionRecord(
                record.id(), record.description(), record.amount(), record.currency(),
                record.lastPayment(), record.isPaid(), workspaceName, record.workspaceId(), ownerName);
    }
}
