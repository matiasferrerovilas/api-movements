package api.m2.movements.services.services;

import api.m2.movements.mappers.ServiceMapper;
import api.m2.movements.records.services.ServiceRecord;
import api.m2.movements.records.services.UpdateServiceRecord;
import api.m2.movements.repositories.ServiceRepository;
import api.m2.movements.services.accounts.AccountQueryService;
import api.m2.movements.services.publishing.websockets.ServicePublishServiceWebSocket;
import api.m2.movements.services.user.UserService;
import io.micrometer.common.util.StringUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UtilitiesService {
    private final ServiceMapper serviceMapper;
    private final ServiceRepository serviceRepository;
    private final UtilityAddService utilityAddService;
    private final UserService userService;
    private final ServicePublishServiceWebSocket servicePublishService;
    private final AccountQueryService accountQueryService;

    public List<ServiceRecord> getServiceBy(List<String> currencySymbol, LocalDate lastPayment) {
        var user = userService.getAuthenticatedUserRecord();
        return serviceRepository.findByCurrencyAndLastPayment(user.id(),
                        currencySymbol,
                        lastPayment).stream()
                .map(serviceMapper::toRecord)
                .toList();
    }

    @Transactional
    public void payServiceById(Long id) {
        var service = serviceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entidad no encontrada"));

        utilityAddService.addMovementService(service);
        service.setLastPayment(LocalDate.now());
        var dto = serviceMapper.toRecord(serviceRepository.save(service));

        servicePublishService.publishServicePaid(dto);
    }

    @Transactional
    public void updateService(Long id, UpdateServiceRecord updateService) {
        var service = serviceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entidad no encontrada"));

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
    public void deleteService(Long id) {
        var service = serviceRepository.findByIdWithCurrency(id)
                .orElseThrow(() -> new EntityNotFoundException("Entidad no encontrada"));
        serviceRepository.delete(service);
        var dto = serviceMapper.toRecord(service);
        servicePublishService.publishDeleteService(dto);
    }
}