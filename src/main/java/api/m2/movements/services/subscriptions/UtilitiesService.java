package api.m2.movements.services.subscriptions;

import api.m2.movements.annotations.RequiresMembership;
import api.m2.movements.entities.Subscription;
import api.m2.movements.enums.MembershipDomain;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.mappers.SubscriptionMapper;
import api.m2.movements.records.services.SubscriptionRecord;
import api.m2.movements.records.services.UpdateSubscriptionRecord;
import api.m2.movements.repositories.MovementRepository;
import api.m2.movements.repositories.SubscriptionRepository;
import api.m2.movements.services.groups.AccountQueryService;
import api.m2.movements.services.publishing.websockets.ServicePublishServiceWebSocket;
import api.m2.movements.services.user.UserService;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UtilitiesService {
    private final SubscriptionMapper serviceMapper;
    private final SubscriptionRepository serviceRepository;
    private final UtilityAddService utilityAddService;
    private final UserService userService;
    private final ServicePublishServiceWebSocket servicePublishService;
    private final AccountQueryService accountQueryService;
    private final MovementRepository movementRepository;

    public List<SubscriptionRecord> getServiceBy(List<String> currencySymbol, LocalDate lastPayment) {
        var user = userService.getAuthenticatedUserRecord();
        return serviceRepository.findByCurrencyAndLastPayment(user.id(),
                        currencySymbol,
                        lastPayment).stream()
                .map(serviceMapper::toRecord)
                .toList();
    }

    @Transactional
    @RequiresMembership(domain = MembershipDomain.SUBSCRIPTION)
    public void payServiceById(Long id) {
        var service = serviceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Servicio no encontrado"));

        service.setLastPayment(LocalDate.now(ZoneOffset.UTC));

        utilityAddService.addMovementService(service);
        var dto = serviceMapper.toRecord(serviceRepository.save(service));

        servicePublishService.publishServicePaid(dto);
    }

    @Transactional
    @RequiresMembership(domain = MembershipDomain.SUBSCRIPTION)
    public void updateService(Long id, UpdateSubscriptionRecord updateService) {
        var service = serviceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Servicio no encontrada"));

        this.syncMovementIfPaid(service, updateService);

        this.updateAccountIfPresent(service, updateService.group());

        serviceMapper.updateMovement(updateService, service);
        service.setLastPayment(updateService.lastPayment());

        var dto = serviceMapper.toRecord(serviceRepository.save(service));
        servicePublishService.publishUpdateService(dto);
    }

    private void syncMovementIfPaid(Subscription service, UpdateSubscriptionRecord update) {
        boolean wasPaid = service.getIsPaid();
        String oldDescription = service.getDescription();
        LocalDate oldLastPayment = service.getLastPayment();

        if (wasPaid) {
            this.syncAssociatedMovement(service, update, oldDescription, oldLastPayment);
        }
    }

    private void updateAccountIfPresent(Subscription service, String group) {
        if (!StringUtils.isEmpty(group)) {
            service.setAccount(accountQueryService.findAccountByName(group));
        }
    }

    private void syncAssociatedMovement(Subscription service, UpdateSubscriptionRecord update,
                                        String oldDescription, LocalDate oldLastPayment) {
        var movement = movementRepository.findByDescriptionAndAccountAndMonth(
                        "Servicio Pagado " + oldDescription,
                        service.getAccount().getId(),
                        oldLastPayment.getYear(),
                        oldLastPayment.getMonthValue())
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el movimiento asociado al servicio"));

        if (update.amount() != null)      movement.setAmount(update.amount());
        if (update.description() != null) movement.setDescription("Servicio Pagado " + update.description());
        if (update.lastPayment() != null) movement.setDate(update.lastPayment());

        movementRepository.save(movement);
    }

    @Transactional
    @RequiresMembership(domain = MembershipDomain.SUBSCRIPTION)
    public void deleteService(Long id) {
        var service = serviceRepository.findByIdWithCurrency(id)
                .orElseThrow(() -> new EntityNotFoundException("Entidad no encontrada"));

        serviceRepository.delete(service);
        var dto = serviceMapper.toRecord(service);
        servicePublishService.publishDeleteService(dto);
    }
}
