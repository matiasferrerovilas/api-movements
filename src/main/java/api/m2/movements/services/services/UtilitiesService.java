package api.m2.movements.services.services;

import api.m2.movements.annotations.RequiresMembership;
import api.m2.movements.enums.MembershipDomain;
import api.m2.movements.mappers.SubscriptionMapper;
import api.m2.movements.records.services.SubscriptionRecord;
import api.m2.movements.records.services.UpdateSubscriptionRecord;
import api.m2.movements.repositories.SubscriptionRepository;
import api.m2.movements.services.groups.AccountQueryService;
import api.m2.movements.services.publishing.websockets.ServicePublishServiceWebSocket;
import api.m2.movements.services.user.UserService;
import io.micrometer.common.util.StringUtils;
import api.m2.movements.exceptions.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

        serviceMapper.updateMovement(updateService, service);
        if (!StringUtils.isEmpty(updateService.group())) {
            var account = accountQueryService.findAccountByName(updateService.group());
            service.setAccount(account);
        }
        if (updateService.lastPayment() == null) {
            service.setLastPayment(null);
        }
        var dto = serviceMapper.toRecord(serviceRepository.save(service));
        servicePublishService.publishUpdateService(dto);
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