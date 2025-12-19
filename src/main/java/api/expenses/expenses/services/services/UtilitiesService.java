package api.expenses.expenses.services.services;

import api.expenses.expenses.mappers.ServiceMapper;
import api.expenses.expenses.records.services.ServiceRecord;
import api.expenses.expenses.records.services.UpdateServiceRecord;
import api.expenses.expenses.repositories.ServiceRepository;
import api.expenses.expenses.services.groups.GroupGetService;
import api.expenses.expenses.services.publishing.websockets.ServicePublishServiceWebSocket;
import api.expenses.expenses.services.user.UserService;
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
    private final GroupGetService groupGetService;

    public List<ServiceRecord> getServiceBy(List<String> currencySymbol, LocalDate lastPayment) {
        var user = userService.getAuthenticatedUserRecord();
        return serviceRepository.findByCurrencyAndLastPayment(user,
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
            var group = groupGetService.getGroupByDescription(updateService.group());
            service.setUserGroups(group);
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