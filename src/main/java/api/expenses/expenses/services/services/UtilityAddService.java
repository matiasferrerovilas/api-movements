package api.expenses.expenses.services.services;

import api.expenses.expenses.aspect.interfaces.PublishMovement;
import api.expenses.expenses.entities.Services;
import api.expenses.expenses.enums.BanksEnum;
import api.expenses.expenses.enums.CategoryEnum;
import api.expenses.expenses.enums.EventType;
import api.expenses.expenses.enums.MovementType;
import api.expenses.expenses.mappers.ServiceMapper;
import api.expenses.expenses.records.movements.MovementToAdd;
import api.expenses.expenses.records.services.ServiceRecord;
import api.expenses.expenses.records.services.ServiceToAdd;
import api.expenses.expenses.repositories.CurrencyRepository;
import api.expenses.expenses.repositories.ServiceRepository;
import api.expenses.expenses.services.accounts.AccountQueryService;
import api.expenses.expenses.services.category.CategoryAddService;
import api.expenses.expenses.services.movements.MovementAddService;
import api.expenses.expenses.services.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

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

    @PublishMovement(eventType = EventType.SERVICE_PAID, routingKey = "/topic/servicios/new")
    public ServiceRecord save(ServiceToAdd serviceToAdd) {
        var user = userService.getAuthenticatedUser();
        var account = accountQueryService.findAccountByName(serviceToAdd.group());
        var service = serviceMapper.toEntity(serviceToAdd, currencyRepository);
        service.setOwner(user);
        service.setAccount(account);

        if (service.getIsPaid()) {
            this.addMovementService(service);
        }
        return serviceMapper.toRecord(serviceRepository.save(service));
    }

    public void addMovementService(Services serviceToAdd) {
        var category = categoryAddService.findCategoryByDescription(CategoryEnum.SERVICIOS.getDescripcion());
        String description = StringUtils.join("Servicio Pagado ", serviceToAdd.getDescription());

        movementAddService.saveMovement(new MovementToAdd(serviceToAdd.getAmount(),
                LocalDate.now(),
                description,
                category.description(),
                MovementType.DEBITO.name(),
                serviceToAdd.getCurrency().getSymbol(),
                0,
                0,
                BanksEnum.GALICIA,
                serviceToAdd.getAccount().getName()));
    }
}