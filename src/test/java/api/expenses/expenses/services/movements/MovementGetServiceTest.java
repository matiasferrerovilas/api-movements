package api.expenses.expenses.services.movements;

import api.expenses.expenses.entities.Movement;
import api.expenses.expenses.mappers.MovementMapper;
import api.expenses.expenses.records.LastIngresoRecord;
import api.expenses.expenses.records.groups.UserRecord;
import api.expenses.expenses.repositories.MovementRepository;
import api.expenses.expenses.services.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovementGetServiceTest {
    @Mock
    private UserService userService;

    @Mock
    private MovementRepository movementRepository;

    @Mock
    private MovementMapper movementMapper;

    @InjectMocks
    private MovementGetService movementGetService;

    private final Long userId = 42L;

    @BeforeEach
    void setup() {
        when(userService.getAuthenticatedUserRecord())
                .thenReturn(new UserRecord("u@example.com", List.of(), userId));
    }



    @Test
    @DisplayName("getLastIngreso devuelve LastIngresoRecord cuando existe un ingreso")
    void getLastIngresoSuccess() {
        var ingresoEntity = mock(Movement.class);
        var record = mock(LastIngresoRecord.class);

        when(movementRepository.getLastIngreso(any())).thenReturn(Optional.of(ingresoEntity));
        when(movementMapper.toLastIngreso(ingresoEntity)).thenReturn(record);

        LastIngresoRecord result = movementGetService.getLastIngreso();

        assertNotNull(result);
        assertEquals(record, result);

        verify(userService).getAuthenticatedUserRecord();
        verify(movementRepository).getLastIngreso(any());
        verify(movementMapper).toLastIngreso(ingresoEntity);
    }
    @Test
    @DisplayName("getLastIngreso lanza EntityNotFoundException si no hay ingreso")
    void getLastIngresoNotFound() {
        when(movementRepository.getLastIngreso(any())).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> movementGetService.getLastIngreso());

        assertEquals("No se encontro ningun ingreso", ex.getMessage());

        verify(userService).getAuthenticatedUserRecord();
        verify(movementRepository).getLastIngreso(any());
        verify(movementMapper, never()).toLastIngreso(any());
    }
}