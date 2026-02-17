package api.m2.movements.services.services;

import api.m2.movements.entities.Services;
import api.m2.movements.enums.BanksEnum;
import api.m2.movements.enums.CategoryEnum;
import api.m2.movements.enums.MovementType;
import api.m2.movements.mappers.ServiceMapper;
import api.m2.movements.records.movements.MovementToAdd;
import api.m2.movements.records.services.ServiceToAdd;
import api.m2.movements.repositories.CurrencyRepository;
import api.m2.movements.repositories.ServiceRepository;
import api.m2.movements.services.accounts.AccountQueryService;
import api.m2.movements.services.category.CategoryAddService;
import api.m2.movements.services.movements.MovementAddService;
import api.m2.movements.services.publishing.websockets.ServicePublishServiceWebSocket;
import api.m2.movements.services.user.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UtilityAddService {
    private final ServiceMapper serviceMapper;
    private final ServiceRepository serviceRepository;
    private final CurrencyRepository currencyRepository;
    private final MovementAddService movementAddService;
    private final CategoryAddService categoryAddService;
    private final UserService userService;
    private final AccountQueryService accountQueryService;
    private final ServicePublishServiceWebSocket servicePublishService;

    @Transactional
    public void save(ServiceToAdd serviceToAdd) {
        var user = userService.getAuthenticatedUser();
        var account = accountQueryService.findAccountById(serviceToAdd.accountId());
        var service = serviceMapper.toEntity(serviceToAdd, currencyRepository);
        service.setOwner(user);
        service.setAccount(account);

        if (service.getIsPaid()) {
            this.addMovementService(service);
        }

        servicePublishService.publishNewService(
                serviceMapper
                        .toRecord(serviceRepository.save(service)));
    }

    public void addMovementService(Services serviceToAdd) {
        var category = categoryAddService.findCategoryByDescription(CategoryEnum.SERVICIOS.getDescripcion());
        String description = StringUtils.join("Servicio Pagado ", serviceToAdd.getDescription());

        movementAddService.saveMovement(new MovementToAdd(serviceToAdd.getAmount(),
                Optional.of(serviceToAdd.getLastPayment()).orElseGet(LocalDate::now),
                description,
                category.description(),
                MovementType.DEBITO.name(),
                serviceToAdd.getCurrency().getSymbol(),
                0,
                0,
                BanksEnum.GALICIA,
                serviceToAdd.getAccount().getId()));
    }
}