package api.m2.movements.services.subscriptions;

import api.m2.movements.entities.commons.Bank;
import api.m2.movements.enums.DefaultCategory;
import api.m2.movements.enums.MovementType;
import api.m2.movements.records.movements.MovementToAdd;
import api.m2.movements.records.subscriptions.SubscriptionMovementSyncEvent;
import api.m2.movements.records.subscriptions.SubscriptionPaidEvent;
import api.m2.movements.services.category.CategoryAddService;
import api.m2.movements.services.movements.MovementAddService;
import api.m2.movements.services.movements.SyncMovementsService;
import api.m2.movements.services.settings.UserSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SubscriptionMovementHandler {

    private final MovementAddService movementAddService;
    private final CategoryAddService categoryAddService;
    private final UserSettingService userSettingService;
    private final SyncMovementsService syncMovementsService;

    @EventListener
    @Transactional
    public void onSubscriptionPaid(SubscriptionPaidEvent event) {
        var category = categoryAddService
                .findCategoryByDescription(DefaultCategory.SERVICIOS.getDescription());

        String defaultBank = userSettingService.getDefaultBank(event.ownerId())
                .map(Bank::getDescription)
                .orElse(null);

        var dto = new MovementToAdd(
                event.amount(),
                event.paymentDate(),
                event.description(),
                category.description(),
                MovementType.DEBITO.name(),
                event.currencySymbol(),
                0,
                0,
                defaultBank);

        movementAddService.saveMovement(dto, event.workspaceId(), event.ownerId());
    }

    @EventListener
    @Transactional
    public void onSubscriptionMovementSync(SubscriptionMovementSyncEvent event) {
        syncMovementsService.syncSubscriptionMovement(event);
    }
}
