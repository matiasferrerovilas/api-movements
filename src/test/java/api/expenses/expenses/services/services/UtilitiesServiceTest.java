package api.expenses.expenses.services.services;

import api.expenses.expenses.entities.Currency;
import api.expenses.expenses.entities.Services;
import api.expenses.expenses.entities.User;
import api.expenses.expenses.entities.UserGroups;
import api.expenses.expenses.enums.CurrencyEnum;
import api.expenses.expenses.enums.GroupsEnum;
import api.expenses.expenses.exceptions.BusinessException;
import api.expenses.expenses.exceptions.PermissionDeniedException;
import api.expenses.expenses.mappers.ServiceMapperImpl;
import api.expenses.expenses.records.currencies.CurrencyRecord;
import api.expenses.expenses.records.groups.UserGroupsRecord;
import api.expenses.expenses.records.groups.UserRecord;
import api.expenses.expenses.records.services.ServiceRecord;
import api.expenses.expenses.records.services.UpdateServiceRecord;
import api.expenses.expenses.repositories.ServiceRepository;
import api.expenses.expenses.services.publishing.websockets.ServicePublishServiceWebSocket;
import api.expenses.expenses.services.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UtilitiesServiceTest {
    @Spy
    private ServiceMapperImpl serviceMapper;
    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private UtilityAddService utilityAddService;
    @Mock
    private UserService userService;
    @Mock
    private ServicePublishServiceWebSocket servicePublishService;


    @InjectMocks
    private UtilitiesService utilitiesService;

    @Test
    @DisplayName("Fallo al obtener los servicios dado que no tengo permisos")
    void getServiceByFail() {
        List<String> currencies = List.of(CurrencyEnum.ARS.name());
        LocalDate lastPayment = LocalDate.now();

        doThrow(new PermissionDeniedException("Usuario no autenticado"))
                .when(userService)
                .getAuthenticatedUserRecord();

        assertThrows(PermissionDeniedException.class,
                () -> utilitiesService.getServiceBy(currencies, lastPayment));
    }

    @Test
    @DisplayName("Obtengo los servicios del usuario correctamente")
    void getServiceByCorrecto() {
        List<String> currencies = List.of(CurrencyEnum.ARS.name());
        LocalDate lastPayment = LocalDate.now();
        var user = new UserRecord("test@gmail.com",
                List.of(new UserGroupsRecord("DEFAULT", 1L)),
                1L);

        when(userService.getAuthenticatedUserRecord())
                .thenReturn(user);
        when(serviceRepository.findByCurrencyAndLastPayment(user,
                currencies,
                lastPayment))
                .thenReturn(List.of(Services.builder()
                        .description("EDESUR")
                        .userGroups(UserGroups.builder().description(GroupsEnum.DEFAULT.name()).build())
                        .amount(new BigDecimal("150.30000"))
                        .build()));
        var result = utilitiesService.getServiceBy(currencies, lastPayment);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());

        verify(userService).getAuthenticatedUserRecord();
        verify(serviceRepository)
                .findByCurrencyAndLastPayment(user, currencies, lastPayment);

    }

    @Test
    @DisplayName("Pago correctamente el servicio")
    void payServiceByOk() {
        Long id = 10L;
        LocalDate today = LocalDate.now();

        var currency = Currency.builder()
                .symbol("ARS")
                .build();
        var serviceEntity = Services.builder()
                .id(id)
                .description("Netflix")
                .currency(currency)
                .lastPayment(today.minusMonths(1))
                .userGroups(UserGroups.builder().description(GroupsEnum.DEFAULT.name()).build())
                .users(User.builder()
                        .email("test@gmail.com")
                        .build())
                .build();
        var expectedRecord = new ServiceRecord(
                id, "Netflix", null, new CurrencyRecord("ARS"),
                LocalDate.now(), true, GroupsEnum.DEFAULT.name(), "test@gmail.com"
        );

        when(serviceRepository.findById(id))
                .thenReturn(Optional.of(serviceEntity));
        when(serviceRepository.save(any(Services.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        utilitiesService.payServiceById(id);

        verify(serviceRepository).findById(id);
        verify(utilityAddService).addMovementService(serviceEntity);
        verify(serviceRepository).save(serviceEntity);
        verify(serviceMapper).toRecord(serviceEntity);
        verify(servicePublishService).publishServicePaid(expectedRecord);
        verifyNoMoreInteractions(servicePublishService, serviceRepository, utilityAddService);

    }

    @Test
    @DisplayName("Fallo al pagar el servicio dado que no existe")
    void payServiceFailNotExists() {
        Long id = 10L;

        when(serviceRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> utilitiesService.payServiceById(id));
    }

    @Test
    @DisplayName("Fallo al pagar el servicio al agregar el movimiento")
    void payServiceFailAddMovement() {
        Long id = 10L;

        var currency = Currency.builder()
                .symbol("ARS")
                .build();
        var serviceEntity = Services.builder()
                .id(id)
                .description("Netflix")
                .currency(currency)
                .lastPayment(LocalDate.now().minusMonths(1))
                .build();

        when(serviceRepository.findById(id))
                .thenReturn(Optional.of(serviceEntity));
        doThrow(new BusinessException("Error al agregar movimiento"))
                .when(utilityAddService)
                .addMovementService(serviceEntity);

        assertThrows(BusinessException.class,
                () -> utilitiesService.payServiceById(id));
    }

    @Test
    @DisplayName("Fallo al actualizar el monto del servicio dado que no existe")
    void updateServiceFailNotExists() {
        Long id = 10L;
        var updateRecord = new UpdateServiceRecord(new BigDecimal("1234.0"),
                null,
                null,
                null);
        when(serviceRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> utilitiesService.updateService(id, updateRecord));
    }

    @Test
    @DisplayName("Actualizo el monto del servicio correctamente")
    void updateServiceOk() {
        Long id = 10L;
        var update = new UpdateServiceRecord(BigDecimal.valueOf(500),
                null,
                null,
                null);

        var currency = Currency.builder()
                .symbol("ARS")
                .description("Pesos Argentinos")
                .build();
        var serviceEntity = Services.builder()
                .id(id)
                .description("Netflix")
                .amount(BigDecimal.valueOf(100))
                .currency(currency)
                .lastPayment(LocalDate.of(2025, 11, 1))
                .build();

        var recordEsperado = new ServiceRecord(
                id,
                "Netflix",
                BigDecimal.valueOf(500),
                new CurrencyRecord("ARS"),
                LocalDate.of(2025, 11, 1),
                false, GroupsEnum.DEFAULT.name(), "test@gmail.com"
        );

        when(serviceRepository.findById(id))
                .thenReturn(Optional.of(serviceEntity));
        when(serviceRepository.save(any(Services.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(serviceMapper.toRecord(any(Services.class)))
                .thenReturn(recordEsperado);

        utilitiesService.updateService(id, update);

        assertEquals(BigDecimal.valueOf(500), serviceEntity.getAmount());

        verify(serviceRepository).findById(id);
        verify(serviceRepository).save(serviceEntity);
        verify(serviceMapper).toRecord(serviceEntity);
        verify(servicePublishService).publishUpdateService(any());

    }

    @Test
    @DisplayName("Fallo al eliminar el servicio dado que no existe")
    void deleteServiceFail() {
        Long id = 10L;

        when(serviceRepository.findByIdWithCurrency(id))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> utilitiesService.deleteService(id));
    }

    @Test
    @DisplayName("Elimino correctamente el servicio")
    void deleteServiceOk() {
        Long id = 10L;
        var currency = Currency.builder()
                .symbol("ARS")
                .build();
        var serviceEntity = Services.builder()
                .id(id)
                .description("Netflix")
                .currency(currency)
                .lastPayment(LocalDate.now().minusMonths(1))
                .build();
        var recordEsperado = new ServiceRecord(
                id,
                "Netflix",
                BigDecimal.valueOf(500),
                new CurrencyRecord("ARS"),
                LocalDate.of(2025, 11, 1),
                false,
                GroupsEnum.DEFAULT.name(), "test@gmail.com"
        );

        when(serviceRepository.findByIdWithCurrency(id))
                .thenReturn(Optional.of(serviceEntity));
        when(serviceMapper.toRecord(any(Services.class)))
                .thenReturn(recordEsperado);

        utilitiesService.deleteService(id);

        verify(serviceRepository).findByIdWithCurrency(id);
        verify(serviceRepository).delete(serviceEntity);
        verify(serviceMapper).toRecord(serviceEntity);
        verify(servicePublishService).publishDeleteService(any());

    }
}