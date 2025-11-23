package api.expenses.expenses.services.services;

import api.expenses.expenses.aspect.interfaces.PublishMovement;
import api.expenses.expenses.enums.EventType;
import api.expenses.expenses.mappers.ServiceMapper;
import api.expenses.expenses.records.services.ServiceRecord;
import api.expenses.expenses.records.services.UpdateServiceRecord;
import api.expenses.expenses.repositories.ServiceRepository;
import api.expenses.expenses.services.user.UserService;
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
public class UtilitiesService  {
    private final ServiceMapper serviceMapper;
    private final ServiceRepository serviceRepository;
    private final UtilityAddService utilityAddService;
    private final UserService userService;

    public List<ServiceRecord> getServiceBy(List<String> currencySymbol, LocalDate lastPayment) {
        var user = userService.getAuthenticatedUserRecord();
        return serviceRepository.findByCurrencyAndLastPayment(user,currencySymbol, lastPayment).stream()
                .map(serviceMapper::toRecord)
                .toList();
    }

    @PublishMovement(eventType = EventType.SERVICE_PAID, routingKey = "/topic/servicios/update")
    @Transactional
    public ServiceRecord payServiceById(Long id) {
        var service = serviceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entidad no encontrada"));

        utilityAddService.addMovementService(service);

        service.setLastPayment(LocalDate.now());
        return serviceMapper.toRecord(serviceRepository.save(service));
    }

    @PublishMovement(eventType = EventType.SERVICE_UPDATED, routingKey = "/topic/servicios/update")
    @Transactional
    public ServiceRecord updateService(Long id, UpdateServiceRecord updateService) {
        var service = serviceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entidad no encontrada"));

        service.setAmount(updateService.amount());
        return serviceMapper.toRecord(serviceRepository.save(service));
    }

    @PublishMovement(eventType = EventType.SERVICE_DELETED, routingKey = "/topic/servicios/remove")
    public ServiceRecord deleteService(Long id) {
        var service = serviceRepository.findByIdWithCurrency(id)
                .orElseThrow(() -> new EntityNotFoundException("Entidad no encontrada"));
        serviceRepository.delete(service);
        return serviceMapper.toRecord(service);
    }
}
