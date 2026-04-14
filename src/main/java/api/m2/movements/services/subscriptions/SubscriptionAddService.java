package api.m2.movements.services.subscriptions;

import api.m2.movements.annotations.RequiresMembership;
import api.m2.movements.entities.Bank;
import api.m2.movements.entities.Subscription;
import api.m2.movements.enums.DefaultCategory;
import api.m2.movements.enums.MembershipDomain;
import api.m2.movements.enums.MovementType;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.mappers.SubscriptionMapper;
import api.m2.movements.records.movements.MovementToAdd;
import api.m2.movements.records.services.SubscriptionToAdd;
import api.m2.movements.records.services.UpdateSubscriptionRecord;
import api.m2.movements.repositories.CurrencyRepository;
import api.m2.movements.repositories.MovementRepository;
import api.m2.movements.repositories.SubscriptionRepository;
import api.m2.movements.services.category.CategoryAddService;
import api.m2.movements.services.movements.MovementAddService;
import api.m2.movements.services.publishing.websockets.ServicePublishServiceWebSocket;
import api.m2.movements.services.settings.UserSettingService;
import api.m2.movements.services.user.UserService;
import api.m2.movements.services.workspaces.WorkspaceContextService;
import api.m2.movements.services.workspaces.WorkspaceQueryService;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final MovementRepository movementRepository;
    private final MovementAddService movementAddService;
    private final CategoryAddService categoryAddService;
    private final UserService userService;
    private final WorkspaceContextService workspaceContextService;
    private final WorkspaceQueryService workspaceQueryService;
    private final ServicePublishServiceWebSocket servicePublishService;
    private final UserSettingService userSettingService;

    @Transactional
    public void save(SubscriptionToAdd subscriptionToAdd) {
        var user = userService.getAuthenticatedUser();
        var workspace = workspaceContextService.getActiveWorkspace();
        var subscription = subscriptionMapper.toEntity(subscriptionToAdd, currencyRepository);
        subscription.setOwner(user);
        subscription.setWorkspace(workspace);

        if (subscription.getIsPaid()) {
            this.addMovementForSubscription(subscription);
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

        this.addMovementForSubscription(subscription);
        var dto = subscriptionMapper.toRecord(subscriptionRepository.save(subscription));

        servicePublishService.publishServicePaid(dto);
    }

    @Transactional
    @RequiresMembership(domain = MembershipDomain.SUBSCRIPTION)
    public void updateSubscription(Long id, UpdateSubscriptionRecord updateSubscription) {
        var subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Servicio no encontrado"));

        this.syncMovementIfPaid(subscription, updateSubscription);
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

    public void addMovementForSubscription(Subscription subscription) {
        var category = categoryAddService
                .findCategoryByDescription(DefaultCategory.SERVICIOS.getDescription());
        String description = "Servicio Pagado " + subscription.getDescription();

        String defaultBank = userSettingService.getDefaultBank(subscription.getOwner())
                .map(Bank::getDescription)
                .orElse(null);

        movementAddService.saveMovement(new MovementToAdd(
                subscription.getAmount(),
                Optional.ofNullable(subscription.getLastPayment()).orElseGet(() -> LocalDate.now(ZoneOffset.UTC)),
                description,
                category.description(),
                MovementType.DEBITO.name(),
                subscription.getCurrency().getSymbol(),
                0,
                0,
                defaultBank));
    }

    private void syncMovementIfPaid(Subscription subscription, UpdateSubscriptionRecord update) {
        boolean wasPaid = subscription.getIsPaid();
        String oldDescription = subscription.getDescription();
        LocalDate oldLastPayment = subscription.getLastPayment();

        if (wasPaid) {
            this.syncAssociatedMovement(subscription, update, oldDescription, oldLastPayment);
        }
    }

    private void updateWorkspaceIfPresent(Subscription subscription, String workspace) {
        if (!StringUtils.isEmpty(workspace)) {
            subscription.setWorkspace(workspaceQueryService.findWorkspaceByName(workspace));
        }
    }

    private void syncAssociatedMovement(Subscription subscription, UpdateSubscriptionRecord update,
                                        String oldDescription, LocalDate oldLastPayment) {
        var movement = movementRepository.findByDescriptionAndAccountAndMonth(
                        "Servicio Pagado " + oldDescription,
                        subscription.getWorkspace().getId(),
                        oldLastPayment.getYear(),
                        oldLastPayment.getMonthValue())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró el movimiento asociado al servicio"));

        if (update.amount() != null) {
            movement.setAmount(update.amount());
        }
        if (update.description() != null) {
            movement.setDescription("Servicio Pagado " + update.description());
        }
        if (update.lastPayment() != null) {
            movement.setDate(update.lastPayment());
        }

        movementRepository.save(movement);
    }
}
