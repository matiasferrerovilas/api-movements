package api.m2.movements.services.services;

import api.m2.movements.entities.Bank;
import api.m2.movements.entities.Subscription;
import api.m2.movements.enums.MovementType;
import api.m2.movements.mappers.SubscriptionMapper;
import api.m2.movements.records.movements.MovementToAdd;
import api.m2.movements.records.services.SubscriptionToAdd;
import api.m2.movements.repositories.CurrencyRepository;
import api.m2.movements.repositories.SubscriptionRepository;
import api.m2.movements.services.groups.AccountQueryService;
import api.m2.movements.services.category.CategoryAddService;
import api.m2.movements.services.movements.MovementAddService;
import api.m2.movements.services.publishing.websockets.ServicePublishServiceWebSocket;
import api.m2.movements.services.settings.UserSettingService;
import api.m2.movements.services.user.UserService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UtilityAddService {
    private final SubscriptionMapper serviceMapper;
    private final SubscriptionRepository serviceRepository;
    private final CurrencyRepository currencyRepository;
    private final MovementAddService movementAddService;
    private final CategoryAddService categoryAddService;
    private final UserService userService;
    private final AccountQueryService accountQueryService;
    private final ServicePublishServiceWebSocket servicePublishService;
    private final UserSettingService userSettingService;

    @Transactional
    public void save(SubscriptionToAdd subscriptionToAdd) {
        var user = userService.getAuthenticatedUser();
        var account = accountQueryService.findAccountById(subscriptionToAdd.groupId());
        var service = serviceMapper.toEntity(subscriptionToAdd, currencyRepository);
        service.setOwner(user);
        service.setAccount(account);

        if (service.getIsPaid()) {
            this.addMovementService(service);
        }

        servicePublishService.publishNewService(
                serviceMapper
                        .toRecord(serviceRepository.save(service)));
    }

    public void addMovementService(Subscription serviceToAdd) {
        var category = categoryAddService.findCategoryByDescription(SERVICIOS);
        String description = StringUtils.join("Servicio Pagado ", serviceToAdd.getDescription());

        String defaultBank = userSettingService.getDefaultBank(serviceToAdd.getOwner())
                .map(Bank::getDescription)
                .orElse(null);

        movementAddService.saveMovement(new MovementToAdd(serviceToAdd.getAmount(),
                Optional.ofNullable(serviceToAdd.getLastPayment()).orElseGet(() -> LocalDate.now(ZoneOffset.UTC)),
                description,
                category.description(),
                MovementType.DEBITO.name(),
                serviceToAdd.getCurrency().getSymbol(),
                0,
                0,
                defaultBank,
                serviceToAdd.getAccount().getId()));
    }

    private static final String SERVICIOS = "SERVICIOS";
}