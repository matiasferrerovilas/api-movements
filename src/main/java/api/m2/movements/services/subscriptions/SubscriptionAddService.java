package api.m2.movements.services.subscriptions;

import api.m2.movements.annotations.RequiresMembership;
import api.m2.movements.entities.movements.Subscription;
import api.m2.movements.enums.MembershipDomain;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.mappers.SubscriptionMapper;
import api.m2.movements.records.services.SubscriptionToAdd;
import api.m2.movements.records.services.UpdateSubscriptionRecord;
import api.m2.movements.records.subscriptions.SubscriptionMovementSyncEvent;
import api.m2.movements.records.subscriptions.SubscriptionPaidEvent;
import api.m2.movements.repositories.CurrencyRepository;
import api.m2.movements.repositories.SubscriptionRepository;
import api.m2.movements.services.publishing.websockets.ServicePublishServiceWebSocket;
import api.m2.movements.services.user.UserService;
import api.m2.movements.services.workspaces.WorkspaceContextService;
import api.m2.movements.services.workspaces.WorkspaceQueryService;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriptionAddService {

    private final SubscriptionMapper subscriptionMapper;
    private final SubscriptionRepository subscriptionRepository;
    private final CurrencyRepository currencyRepository;
    private final UserService userService;
    private final WorkspaceContextService workspaceContextService;
    private final WorkspaceQueryService workspaceQueryService;
    private final ServicePublishServiceWebSocket servicePublishService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void save(SubscriptionToAdd subscriptionToAdd) {
        var user = userService.getAuthenticatedUser();
        var workspace = workspaceContextService.getActiveWorkspace();
        var subscription = subscriptionMapper.toEntity(subscriptionToAdd, currencyRepository);
        subscription.setOwner(user);
        subscription.setWorkspace(workspace);

        if (subscription.getIsPaid()) {
            this.publishPaidEvent(subscription);
        }

        servicePublishService.publishNewService(
                subscriptionMapper.toRecord(subscriptionRepository.save(subscription)));
    }

    @Transactional
    @RequiresMembership(domain = MembershipDomain.SUBSCRIPTION)
    public void paySubscriptionById(Long id) {
        var subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Servicio no encontrado"));

        subscription.setLastPayment(LocalDate.now(ZoneOffset.UTC));
        this.publishPaidEvent(subscription);

        var dto = subscriptionMapper.toRecord(subscriptionRepository.save(subscription));
        servicePublishService.publishServicePaid(dto);
    }

    @Transactional
    @RequiresMembership(domain = MembershipDomain.SUBSCRIPTION)
    public void updateSubscription(Long id, UpdateSubscriptionRecord updateSubscription) {
        var subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Servicio no encontrado"));

        this.publishSyncEventIfPaid(subscription, updateSubscription);
        this.updateWorkspaceIfPresent(subscription, updateSubscription.workspace());

        subscriptionMapper.updateMovement(updateSubscription, subscription);
        subscription.setLastPayment(updateSubscription.lastPayment());

        var dto = subscriptionMapper.toRecord(subscriptionRepository.save(subscription));
        servicePublishService.publishUpdateService(dto);
    }

    @Transactional
    @RequiresMembership(domain = MembershipDomain.SUBSCRIPTION)
    public void deleteSubscription(Long id) {
        var subscription = subscriptionRepository.findByIdWithCurrency(id)
                .orElseThrow(() -> new EntityNotFoundException("Entidad no encontrada"));

        subscriptionRepository.delete(subscription);
        var dto = subscriptionMapper.toRecord(subscription);
        servicePublishService.publishDeleteService(dto);
    }

    private void publishPaidEvent(Subscription subscription) {
        eventPublisher.publishEvent(new SubscriptionPaidEvent(
                subscription.getAmount(),
                Optional.ofNullable(subscription.getLastPayment())
                        .orElseGet(() -> LocalDate.now(ZoneOffset.UTC)),
                "Servicio Pagado " + subscription.getDescription(),
                subscription.getCurrency().getSymbol(),
                subscription.getOwner(),
                subscription.getWorkspace()));
    }

    private void publishSyncEventIfPaid(Subscription subscription, UpdateSubscriptionRecord update) {
        if (!subscription.getIsPaid()) {
            return;
        }
        var oldLastPayment = subscription.getLastPayment();
        eventPublisher.publishEvent(new SubscriptionMovementSyncEvent(
                subscription.getDescription(),
                subscription.getWorkspace().getId(),
                oldLastPayment.getYear(),
                oldLastPayment.getMonthValue(),
                update.amount(),
                update.description(),
                update.lastPayment()));
    }

    private void updateWorkspaceIfPresent(Subscription subscription, String workspace) {
        if (!StringUtils.isEmpty(workspace)) {
            subscription.setWorkspace(workspaceQueryService.findWorkspaceByName(workspace));
        }
    }
}
